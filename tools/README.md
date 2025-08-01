# Tools

This directory contains utility scripts and tools for the DeusExMachina project.

## verify.sh

A comprehensive verification script for Java backend code that performs:
- JML (Java Modeling Language) formal verification using OpenJML
- Static analysis with SpotBugs
- Error Prone analysis (via Gradle build)

### Prerequisites

1. **OpenJML**: Download from https://www.openjml.org/
   - Set the `OPENJML_JAR` environment variable to point to the OpenJML JAR file
   - Example: `export OPENJML_JAR=/usr/local/lib/openjml.jar`

2. **Java 21**: Required for building and running the backend code

### Usage

From the project root directory:

```bash
./tools/verify.sh
```

### What it does

1. **Builds all Java modules** to ensure code is compiled
2. **Runs JML verification** on each backend module:
   - auth-function
   - api-function
   - processor-function
   - shared utilities
3. **Performs static analysis** using SpotBugs
4. **Generates reports** in the `reports/verification/` directory
5. **Creates a summary** of all verification results

### Output

The script generates several reports:
- `reports/verification/{module}-jml-report.txt`: JML verification results for each module
- `reports/verification/spotbugs-report.txt`: SpotBugs static analysis results
- `reports/verification/verification-summary.txt`: Summary of all verification results

### Exit Codes

- `0`: All verifications passed
- `1`: Verification errors found or build failed

### Customization

You can modify the script to:
- Add additional modules to verify
- Include other static analysis tools
- Change report formats
- Adjust verification strictness

## Adding New Tools

When adding new tools to this directory:
1. Create the script with a descriptive name
2. Make it executable: `chmod +x script-name.sh`
3. Add documentation in this README
4. Include usage examples and prerequisites