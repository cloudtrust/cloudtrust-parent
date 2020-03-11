pipeline {
  agent any
  options {
    timestamps()
    timeout(time: 3600, unit: 'SECONDS')
  }
  parameters {
    string(name: 'BROWSER', defaultValue: 'htmlunit')
    string(name: 'SKIP_TESTS', defaultValue: 'false')
  }
  stages {
    stage('Build') {
      agent {
        label 'jenkins-slave-maven-ct'
      }
      steps {
        script {
          sh 'printenv'
          def options = ""
          def prefix = ""
          if (params.BROWSER == "chrome") {
            options = '-DchromeOptions="--headless --no-sandbox --disable-setuid-sandbox --disable-gpu --disable-software-rasterizer --remote-debugging-port=9222 --disable-infobars"'
            prefix = 'xvfb-run --server-args="-screen 0 1920x1080x24" --server-num=99'
          } else if (params.BROWSER == "firefox") {
            options = '-DchromeOptions="-headless"'
            prefix = 'xvfb-run --server-args="-screen 0 1920x1080x24" --server-num=99'
          }

          withCredentials([usernamePassword(credentialsId: 'sonarqube', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            def sonar_opts = "\"-Dsonar.login=${USER}\" \"-Dsonar.password=${PASS}\""
            sh """
              ${prefix} mvn -B -T4 clean package \
                -Dbrowser=\"${params.BROWSER}\" \
                ${options} \
                -DskipTests=${params.SKIP_TESTS} \
                spotbugs:spotbugs pmd:pmd dependency-check:check \
                -Dsonar.java.spotbugs.reportPaths=target/spotbugsXml.xml \
                -Dsonar.java.pmd.reportPaths=target/pmd.xml \
                ${sonar_opts} \
                sonar:sonar \
                install deploy \
                -DaltDeploymentRepository=artifactory::default::https://artifactory-test.multi.west.ch.elca-cloud.net/artifactory/cloudtrust-mvn/
            """
          }
        }
      }
    }
  }
}
