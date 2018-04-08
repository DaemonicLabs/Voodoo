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
	    stage("hex") {
	        steps {
	            sh './gradlew :hex:clean'
	            sh './gradlew :hex:build'
	            archive 'hex/build/libs/*jar'
	        }
	    }
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean'
	            sh './gradlew :voodoo:build'
	            archive 'voodoo/build/libs/*jar'
	        }
	    }
	}
}