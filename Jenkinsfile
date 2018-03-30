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
	    stage("pack") {
	        steps {
	            sh './gradlew :pack:clean'
	            sh './gradlew :pack:build'
	            archive 'pack/build/libs/*jar'
	        }
	    }
	}
}