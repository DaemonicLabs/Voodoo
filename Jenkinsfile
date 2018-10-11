pipeline {
    agent any
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
                sh 'rm private.gradle || true'
	            sh './gradlew clean'
	        }
	    }
	    stage("voodoo") {
	        steps {
	            sh './gradlew clean'
	            sh './gradlew test'
	            // archiveArtifacts artifacts:  'build/libs/*jar'
	        }
	    }
	    stage("multimc-installer") {
	        steps {
	            sh './gradlew :multimc:multimc-installer:clean'
	            sh './gradlew :multimc:multimc-installer:shadowJar'
	            archiveArtifacts artifacts:  'multimc/installer/build/libs/*jar'
	        }
	    }
	    stage("server-installer") {
	        steps {
	            sh './gradlew :server-installer:clean'
	            sh './gradlew :server-installer:shadowJar'
	            archiveArtifacts artifacts:  'server-installer/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            // sh './gradlew :bootstrap:shadowJar -Ptarget=voodoo'
	            sh './gradlew :bootstrap:shadowJar -Ptarget=multimc-installer'
	            // archiveArtifacts artifacts:  'bootstrap/build/libs/*voodoo*'
	            archiveArtifacts artifacts:  'bootstrap/build/libs/*multimc-installer*'
	        }
	    }
	    stage('Deploy') {
            steps {
                withCredentials([file(credentialsId: 'privateGradlePublish', variable: 'PRIVATEGRADLE')]) {
                    sh '''
                        cp "$PRIVATEGRADLE" private.gradle
                        ./gradlew publish
                    '''
                }
            }
        }
	}
}