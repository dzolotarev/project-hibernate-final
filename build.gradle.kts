plugins {
    id("java")
}

group = "ru.dzolotarev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.hibernate:hibernate-core-jakarta:5.6.15.Final")
    implementation("p6spy:p6spy:3.9.1")
    implementation("io.lettuce:lettuce-core:6.2.7.RELEASE")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.3")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}