node {
	checkout scm
	sh './gradlew clean :build :bootstrap:build'
	archive 'build/libs/*jar'
	archive 'bootstrap/build/libs/*jar'
}