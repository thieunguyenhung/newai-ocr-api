package vn.newai.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mailjet.client.Base64;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Email;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import vn.newai.core.OCR;
import vn.newai.extraction.FileWriter;

@Path("/upload")
public class FileUploadService {

	/** The path to the folder where we want to store the uploaded files */
	private static final String UPLOAD_FOLDER = FileUploadService.class.getResource("../../../../../").getPath() + "uploads/";
	/** Path to folder that contains tessdata */
	private static final String TESSDATA_PATH = FileUploadService.class.getResource("../../../../../").getPath();
	/** Path to folder that contains configuration file (newai_ocr.ini) */
	private static final String CONFIG_PATH = FileUploadService.class.getResource("../../../../../").getPath() + "config/newai_ocr.ini";

	/** MailJet API Key */
	private String mailjetApiKey = "5522d972a29002a12ae518b64a6056da";
	/** MailJet Secret Key */
	private String mailjetSecretKey = "bc95e9f47aca50bcb3b50d9bf5da8183";

	/** Return type enum for HTML */
	private static final String RETURN_TYPE_HTML = "html";
	/** Return type enum for Microsoft Word */
	private static final String RETURN_TYPE_WORD = "word";
	/** Return type enum Microsoft Excel */
	private static final String RETURN_TYPE_EXCEL = "excel";

	public FileUploadService() {
	}

	private boolean readConfig() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CONFIG_PATH), "UTF8"));
			String line = br.readLine();
			if (null != line)
				mailjetApiKey = line;
			line = br.readLine();
			if (null != line)
				mailjetSecretKey = line;
			br.close();
			System.out.println("Read newai_ocr config sucessfully");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Read newai_ocr config failed");
		}
		return false;
	}

	@Context
	private UriInfo context;

	/**
	 * Returns text response to caller containing current time-stamp
	 * 
	 * @return error response in case of missing parameters an internal
	 *         exception or success response if file has been stored
	 *         successfully
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("lang") String language, @FormDataParam("outputFormat") String outputFormat, @FormDataParam("email") String email) {
		// Read configuration file
		readConfig();
		// check if all form parameters are provided
		if (uploadedInputStream == null || fileDetail == null)
			return Response.status(400).entity("Invalid form data").build();

		// create our destination folder, if it not exists
		try {
			createFolderIfNotExists(UPLOAD_FOLDER);
		} catch (SecurityException se) {
			return Response.status(500).entity("Can not create destination folder on server").build();
		}

		String uploadedFileLocation = UPLOAD_FOLDER + fileDetail.getFileName();

		File uploaded = new File(uploadedFileLocation);

		try {
			saveToFile(uploadedInputStream, uploadedFileLocation);
		} catch (IOException e) {
			return Response.status(500).entity("Can not save file").build();
		}

		if (uploaded.exists()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					OCR ocr = new OCR(uploaded, language, TESSDATA_PATH);
					switch (outputFormat) {
					case RETURN_TYPE_WORD:
						FileWriter.writeWordFile(ocr.getCombinedMicrosoftWord(), uploaded.getAbsolutePath() + ".docx");
						sendEmail(uploaded.getName() + ".docx", uploaded.getAbsolutePath() + ".docx", email);
						break;
					case RETURN_TYPE_EXCEL:
						FileWriter.writeExcelFile(ocr.getCombinedMicrosoftExcel(), uploaded.getAbsolutePath() + ".xlsx");
						sendEmail(uploaded.getName() + ".xlsx", uploaded.getAbsolutePath() + ".xlsx", email);
						break;
					case RETURN_TYPE_HTML:
						FileWriter.writeHTMLFile(ocr.getCombinedHTML(), uploaded.getAbsolutePath() + ".html");
						sendEmail(uploaded.getName() + ".html", uploaded.getAbsolutePath() + ".html", email);
						break;
					default:
						FileWriter.writeHTMLFile(ocr.getCombinedHTML(), uploaded.getAbsolutePath() + ".html");
						sendEmail(uploaded.getName() + ".html", uploaded.getAbsolutePath() + ".html", email);
						break;
					}
					// Remove temporary directory
					try {
						ocr.deleteDirectory();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
		return Response.status(200).entity("OCR result will send to " + email).build();
	}

	/**
	 * Utility method to save InputStream data to target location/file
	 * 
	 * @param inStream
	 *            - InputStream to be saved
	 * @param target
	 *            - full path to destination file
	 */
	private void saveToFile(InputStream inStream, String target) throws IOException {
		OutputStream out = null;
		int read = 0;
		byte[] bytes = new byte[1024];

		out = new FileOutputStream(new File(target));
		while ((read = inStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
	}

	/**
	 * Creates a folder to desired location if it not already exists
	 * 
	 * @param dirName
	 *            - full path to the folder
	 * @throws SecurityException
	 *             in case you don't have permission to create the folder
	 */
	private void createFolderIfNotExists(String dirName) throws SecurityException {
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdir();
			System.out.println("created" + dirName);
		}
	}

	private void sendEmail(String fileName, String filePath, String recipient) {
		try {
			String attachmentContent = Base64.encode(FileUtils.readFileToByteArray(new File(filePath)));

			MailjetClient client = new MailjetClient(mailjetApiKey, mailjetSecretKey);
			MailjetRequest email = new MailjetRequest(Email.resource).property(Email.FROMEMAIL, "noreply@newai.vn").property(Email.FROMNAME, "NewAI").property(Email.SUBJECT, "Your OCR result").property(Email.HTMLPART, "Hi,<br>Thanks for using our service. Please check your result in attachment.").property(Email.RECIPIENTS, new JSONArray().put(new JSONObject().put("Email", recipient))).property(Email.ATTACHMENTS, new JSONArray().put(new JSONObject().put("Content-type", "text/html; charset=UTF-8").put("Filename", fileName).put("content", attachmentContent)));

			// trigger the API call
			MailjetResponse response = client.post(email);
			// Read the response data and status
			System.out.println("Email was sent to " + recipient + ", response: " + response.getStatus());
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (MailjetException e) {
			System.out.println("Mailjet Exception: " + e);
		}
	}
}
