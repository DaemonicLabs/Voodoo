pipeline {
    agent any
	stages {
	    stage("voodoo") {
	        steps {
	            sh './gradlew :flatten:clean'
	            sh './gradlew :flatten:build'
	            archive 'flatten/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :builder:clean'
	            sh './gradlew :builder:build'
	            archive 'builder/build/libs/*jar'
	        }
	    }
	}
}