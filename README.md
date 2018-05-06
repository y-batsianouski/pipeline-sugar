# pipeline-sugar

Pipeline [shared library](https://jenkins.io/doc/book/pipeline/shared-libraries/) which provides declarative syntax sugar for scripted pipeline, but doesn't limit usage of scripted pipeline directives

## Example usage

```groovy
@Library("pipeline-sugar") _
block {

  // Same as 'parameters' from 'properties' pipeline step
  parameters([
    string(defaultValue: 'string',
           description: 'Some param',
           name: 'PARAM_NAME',
           trim: false)
  ])

  // Same as 'properties' pipeline step, excluding 'parameters' and 'pipelineTriggers'
  options([
    buildDiscarder(logRotator(artifactDaysToKeepStr: '5',
                              artifactNumToKeepStr: '5',
                              daysToKeepStr: '10',
                              numToKeepStr: '10')),
    disableConcurrentBuilds(),
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]
  ])

  // Same as 'pipelineTriggers' from 'properties' pipeline step
  triggers([
    cron('H * * * *')
  ])

  // Same as 'wrap' syntax, but could contain multiple wrappers in list
  wrappers([
    [$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm'],
    [$class: 'TimestamperBuildWrapper']
  ])

  // Same as 'tool' syntax, but could contain multiple tools in List
  tools([
    [name: 'Default', type: 'git']
  ])

  // The same as 'withCredentials' syntax
  credentials([
    string(credentialsId: 'credentials_id', variable: 'TOKEN')
  ])

  // Currently only 'label' and 'podTemplate' are supported
  agent {
    podTemplate(label: "my-label")
  }

  // Set environment variables inside block
  environment {
    TEST_VAR = "value"
    TEST_VAR2 = 'value2'
  }

  // Steps could contain any scripted pipeline code, even another blocks
  steps {
    // Block could include another block
    // If block name provided, all block steps will be wrapped into pipeline stage
    block("tests") {
      // Any pipeline code which should return 'true' or 'false'
      when { env.BRANCH_NAME == 'qa' }

      // You could use 'parallel' instead of 'steps' directive in block
      // Only 'stage', 'block', 'parallel' should be used inside parallel step
      parallel {
        block {
          environment {
            TEST_VAR3 = "value3"
          }
          steps {
            stage('prepare for tests') {
              echo "prepare for tests"
            }
            // Example of nested parallel
            parallel {
              stage('parallel test1') {
                echo env.TEST_VAR2
              }
              stage('parallel test2') {
                echo env.TEST_VAR3
              }
            }
          }
          post {
            always {
              echo "nested block post always"
            }
          }
          // Don't raise error if block failed
          ignoreFailure true
        }

        stage('parallel test3') {
          echo env.PARAM_NAME
        }
      }
      post {
        failure { echo "tests failed" }
        success { echo "tests successfuly passed" }
      }
    }

    block("deployment") {
      when { env.BRANCH_NAME == "master" }
      // Example of additional job parameter
      // Will be added to job only if current block will be executed
      parameters([
        booleanParam(defaultValue: false, description: '', name: 'DEPLOY_TO_PROD')
      ])
      steps {
        echo "deployment to prod"
      }
      post {
        success { 'Production deployment finished' }
      }
    }
  }
  post {
    always   { echo "always" }
    success  { echo "success" }
    unstable { echo "unstable" }
    failure  { echo "failure" }
    abort    { echo "aborted" }
  }
}
```
