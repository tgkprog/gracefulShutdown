# Java Version Configuration

## Overview
Successfully configured all three microservices (Central, PROCR, Adapter) to compile with Java 21 bytecode while running on Java 26 (26.0.1).

## Configuration

### pom.xml Changes (All Three Servers)

**Properties:**
```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

**Maven Compiler Plugin:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

### Why Java 21?

1. **Spring Boot 3.5.0 Compatibility**: Fully supports Java 21 bytecode (class file version 65)
2. **Forward Compatible**: Java 21 code runs without issues on Java 26 runtime
3. **Stability**: Java 21 is LTS (Long-Term Support) with wide ecosystem support
4. **No Breaking Changes**: Moving from Java 21 to Java 26 at runtime doesn't require code changes

## Build & Runtime

### Compilation (mvn clean package)
- Files compiled to Java 21 bytecode (major version 65)
- Compiler used: Maven 3.13.0
- No runtime version mismatch issues with Spring Boot ASM parser

### Execution (./run script)
- Runtime JVM: Java 26.0.1
- Backwards compatible with Java 21 bytecode
- All servers start successfully

## Verification

### Class File Versions (Compiled)
```bash
$ javap -verbose central/target/classes/com/gracefulshutdown/central/CentralServerApplication.class | grep "major version"
  major version: 65  # Java 21

$ javap -verbose procr/target/classes/com/gracefulshutdown/procr/ProcrApplication.class | grep "major version"
  major version: 65  # Java 21

$ javap -verbose adapter/target/classes/com/gracefulshutdown/adapter/AdapterApplication.class | grep "major version"
  major version: 65  # Java 21
```

### Runtime JVM
```bash
$ java -version
openjdk version "26.0.1" 2026-04-21
OpenJDK Runtime Environment (build 26.0.1+8-34)
OpenJDK 64-Bit Server VM (build 26.0.1+8-34, mixed mode, sharing)
```

### Startup Test
```bash
$ cd central && ./run
🚀 Starting Central Server (port 5000)
...
03:00:33.353 [main] INFO  c.g.central.CentralServerApplication - Starting CentralServerApplication v1.0.0 using Java 26.0.1 with PID 52484
...
03:00:48.445 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port 5000 (http) with context path ''
03:00:48.446 [main] INFO  c.g.central.CentralServerApplication - Started CentralServerApplication in 15.063 seconds
✅ Server started successfully
```

## Why Not Java 26 Bytecode?

Attempted to compile directly to Java 26 (major version 70):
- ❌ Spring Boot 3.5.0's ASM library doesn't understand Java 26 bytecode
- ✅ Spring Boot 3.6.0+ would support it, but version not yet released
- ✓ Solution: Compile to Java 21, run on Java 26 (works perfectly)

## Files Modified

1. **central/pom.xml**
   - Updated Spring Boot parent 3.4.0 → 3.5.0
   - Updated spring-boot-maven-plugin 3.4.0 → 3.5.0
   - Set compiler target to 21

2. **procr/pom.xml**
   - Updated spring-boot.version 3.5.0
   - Set compiler target to 21

3. **adapter/pom.xml**
   - Updated spring-boot.version 3.5.0
   - Set compiler target to 21

## Summary

| Aspect | Value |
|--------|-------|
| **Compilation Target** | Java 21 (major version 65) |
| **Runtime JVM** | Java 26.0.1 |
| **Spring Boot** | 3.5.0 |
| **Maven Compiler** | 3.13.0 |
| **Build Status** | ✅ All servers build successfully |
| **Runtime Status** | ✅ All servers start and run successfully |
| **Backwards Compatibility** | ✅ Yes, Java 21 bytecode runs on Java 26 |
