pipeline {
    agent any
	stages {
	    stage("flatten") {
	        steps {
	            sh './gradlew :flatten:clean'
	            sh './gradlew :flatten:build'
	            archive 'flatten/build/libs/*jar'
	        }
	    }
	    stage("builder") {
	        steps {
	            sh './gradlew :builder:clean'
	            sh './gradlew :builder:build'
	            archive 'builder/build/libs/*jar'
	        }
	    }
	}
}