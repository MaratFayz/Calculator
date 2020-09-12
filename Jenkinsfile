pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

//    environment {
//        PYTHONPATH = "${WORKSPACE}/section_4/code/cd_pipeline"
//    }

    tools {
        maven "apache-maven-3.6.3" //The tool name must be pre-configured in Jenkins under Manage Jenkins → Global Tool Configuration.
    }

    stages {
        stage("Test - Unit tests") {
            steps {
                runUnittests()
            }
        }

        stage('Build') {
            steps {
                // Get some code from a GitHub repository
                git 'https://github.com/jglick/simple-maven-project-with-tests.git'

                // Run Maven on a Unix agent.
                sh "mvn -Dmaven.test.failure.ignore=true clean package"

                // To run Maven on a Windows agent, use
                // bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }

//            post {
//                // If Maven was able to run the tests, even if some of the test
//                // failed, record the test results and archive the jar file.
//                success {
//                    junit '**/target/surefire-reports/TEST-*.xml'
//                    archiveArtifacts 'target/*.jar'
//                }
//            }
        }
        stage('Test') {
            steps {
                echo 'Testing..'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }

    post {
        unsuccessful {
            echo 'Building failed!'
        }
        successful {
            echo 'Building succeed!'
        }
        always {
            echo 'Jenkins finished building!'
        }
    }
}

// steps
def buildApp() {
    dir ('section_4/code/cd_pipeline' ) {
        def appImage = docker.build("hands-on-jenkins/myapp:${BUILD_NUMBER}")
    }
}


def deploy(environment) {

    def containerName = ''
    def port = ''

    if ("${environment}" == 'dev') {
        containerName = "app_dev"
        port = "8888"
    }
    else if ("${environment}" == 'stage') {
        containerName = "app_stage"
        port = "88"
    }
    else if ("${environment}" == 'live') {
        containerName = "app_live"
        port = "80"
    }
    else {
        println "Environment not valid"
        System.exit(0)
    }

    sh "docker ps -f name=${containerName} -q | xargs --no-run-if-empty docker stop"
    sh "docker ps -a -f name=${containerName} -q | xargs -r docker rm"
    sh "docker run -d -p ${port}:5000 --name ${containerName} hands-on-jenkins/myapp:${BUILD_NUMBER}"

}


def approve() {

    timeout(time:1, unit:'DAYS') {
        input('Do you want to deploy to live?')
    }

}


def runUnittests() {
    sh "pip3 install --no-cache-dir -r ./section_4/code/cd_pipeline/requirements.txt"
    sh "python3 section_4/code/cd_pipeline/tests/test_flask_app.py"
}


def runUAT(port) {
    sh "section_4/code/cd_pipeline/tests/runUAT.sh ${port}"
}
