pipeline {
    agent any
	stages {
	    stage("flatten") {
	        steps {
	            sh './gradlew :flatten:clean :flatten:build'
	            archive 'flatten/build/libs/*jar'
	        }
	    }
	    stage("builder") {
	        steps {
	            sh './gradlew :builder:clean :builder:build'
	            archive 'builder/build/libs/*jar'
	        }
	    }
	    stage("pack") {
	        steps {
	            sh './gradlew :pack:clean :pack:build'
	            archive 'pack/build/libs/*jar'
	        }
	    }
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean :voodoo:build'
	            archive 'voodoo/build/libs/*jar'
	        }
	    }
	}
}