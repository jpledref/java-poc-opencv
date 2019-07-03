# Java poc opencv

Poc application that pops a stream video to http://localhost:8085 using opencv for face recognition

OpenCV, or Open Source Computer Vision library is a library written in C++ programming language.
This native library is platform-dependent: a compiled code from Windows won't run in Linux.

To take the advantage of it with java, a java dependency is required to make the interface through JNI mechanism with the library.
To build both the dependency and the native library, follow the link https://opencv.org/releases/.
(For Windows users', these needed files are already packed with the sources.)


## Build from sources
Build the sources:

	mvn clean install
	
Run the app

	java -Xmx500M -jar java-poc-opencv-0.0.1-SNAPSHOT.jar	

Browse to:

	http://localhost:8085
	
## Run it as a background task

	nohup java -Xms50m -Xmx200m -jar java-poc-opencv-0.0.1-SNAPSHOT.jar 2>&1 >/dev/null &	

	
## Build OpenCV on raspberry pi
	apt-get install ant
	apt-get install build-essential cmake pkg-config libpng12-0 libpng12-dev libpng++-dev
	export JAVA_HOME="/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt"
	cmake -D CMAKE_BUILD_TYPE=RELEASE -D CMAKE_INSTALL_PREFIX=/usr/local -D BUILD_EXAMPLES=OFF ..
	make


## Missing OpenCV dependency with Maven

	mvn install:install-file -DgroupId=org.openpnp -DartifactId=opencv -Dversion=4.1.0 -Dpackaging=jar -Dfile=${basedir}/src/main/resources/opencv-410.jar -DgeneratePom=true
	