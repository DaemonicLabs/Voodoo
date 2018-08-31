pipeline {
    agent any
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
	            sh './gradlew clean'
	        }
	    }
	    stage("test") {
	        steps {
	            sh './gradlew clean'
	            sh './gradlew test'
	        }
	    }
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean'
	            sh './gradlew :voodoo:build'
	            archiveArtifacts artifacts:  'voodoo/build/libs/*jar'
	        }
	    }
	    stage("multimc-installer") {
	        steps {
	            sh './gradlew :multimc:installer:clean'
	            sh './gradlew :multimc:installer:minify'
	            archiveArtifacts artifacts:  'multimc/installer/build/libs/*jar'
	        }
	    }
	    stage("server-installer") {
	        steps {
	            sh './gradlew :server-installer:clean'
	            sh './gradlew :server-installer:minify'
	            archiveArtifacts artifacts:  'server-installer/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            sh './gradlew :bootstrap:minify -Ptarget=voodoo'
	            sh './gradlew :bootstrap:minify -Ptarget=hex'
	            archiveArtifacts artifacts:  'bootstrap/build/libs/*voodoo*'
	            archiveArtifacts artifacts:  'bootstrap/build/libs/*hex*'
	        }
	    }
	}
}