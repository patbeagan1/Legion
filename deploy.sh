: << COMMENT

## Secrets

keys defined as env vars
repo defined in ~/.m2/settings.xml

## Resources

- https://medium.com/@othmane.outama/step-by-step-publishing-java-libraries-to-maven-central-abf2ffeb04c0
- https://kotlinlang.org/docs/dokka-maven.html#build-javadoc-jar
- https://sdkman.io/usage
- https://maven.apache.org/plugins/maven-gpg-plugin/usage.html
- https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html
- https://central.sonatype.com/?smo=true
- https://central.sonatype.com/publishing/deployments
- https://repo1.maven.org/maven2/io/github/patbeagan1/legion/

COMMENT

mvn clean dokka:javadocJar deploy -P deployment
