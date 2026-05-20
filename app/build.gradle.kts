plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // Driver JDBC per MySQL - permette a Java di parlare con MySQL Workbench
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

application {
    // Punto di ingresso dell'applicazione
    mainClass.set("com.piadineria.App")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

