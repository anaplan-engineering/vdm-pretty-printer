import com.anaplan.buildtools.jenkins_pipelines.ContainerTemplates
import com.anaplan.buildtools.jenkins_pipelines.DefaultConfig

@Library('Anaplan_Pipeline')

def BUILD_LABEL = "vdm-pretty-printer.${UUID.randomUUID().toString()}"

pipeline {
    agent {
        kubernetes {
            label BUILD_LABEL
            yaml pod([ContainerTemplates.gradle([:], "8-jdk")])
        }
    }

    parameters {
        text(
            description: 'Version to publish',
            name: 'version'
        )
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '60'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Build & Test') {
            steps {
                script {
                    container('gradle') {
                        try {
                            sh "./gradlew check"
                        } finally {
                            junit '**/build/test-results/**/*.xml'
                        }
                    }
                }
            }
        }
    }

}
