pipeline {
    agent any
    // environment {
    //     GRADLE_OPTS = '-Dkotlin.compiler.execution.strategy="in-process"'
    // }
	options {
		buildDiscarder(logRotator(artifactNumToKeepStr: '10'))
	}
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
	        }
	    }
		stage("test") {
			steps {
                sh './gradlew clean'
				sh './gradlew test -S'
			}
		}
	    stage('publish') {
            steps {
                sh './gradlew publish -S'
            }
        }
		stage('counter') {
			steps {
				sh './gradlew buildnumberIncrement'
			}
		}
	}
}