# Tech. Stack

1. scala, akka core, akka persistence, akka http
2. scala.js, jQuery
3. bootstrap

# Build & Run

## Prerequisites

1. Java 8
2. Internet connection (to download dependencies)

## Run

1. clone code
```git clone https://github.com/happyschemer/gsl.git```

2. change path to the root folder
```cd gsl```

3. build & run server 
```bin/activator -Dgsl.port=8080 "project service" run```

- *gsl.port is defaulted to 80 if it is not explicitly set*
- *you might get JAVA not found error, in that case set JAVA_HOME properly*

4. open browser, and connect to http://host:port (e.g. http://localhost:8080)