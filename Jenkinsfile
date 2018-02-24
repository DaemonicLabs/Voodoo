pipeline {
    agent any
	stages {
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean'
	            sh './gradlew :voodoo:build'
	            archive 'voodoo/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            sh './gradlew :bootstrap:build'
	            archive 'bootstrap/build/libs/*jar'
	        }
	    }
	}
}