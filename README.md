File upload RESTful API
===================

A Java project RESTful service to receive file on server.


Download
-------------
Clone

Usage
-------------
- Clone and deploy on Apache Tomcat Server.
- Send multipart/form-data request to this server

Note
-------------
Path to below directories */src/main/webapp/*<br>
- **config/** folder contains setting string
  - **newai_ocr.ini** file contains mail jet key
- **opencv-native-lib/** folder contains opencv native lib, do **NOT** remove this folder
- **tessdata/** folder contains trained data for tesseract

Dependency 
-------------
- tess4j-4.0.jar
- opencv-320.jar

License 
-------------
- [Apache license 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [elastic Open Source license](https://www.elastic.co/subscriptions)