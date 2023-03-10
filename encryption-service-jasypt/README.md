# MDX Dependency - Encryption Service Vault

Vault encryption service implementation

## Gateway Configuration

```yaml
encryptionService:
  class: com.mx.path.service.facility.encryption.jasypt.JasyptEncryptionService
  configurations:
    enabled: true
    poolSize: 10
    currentKeyIndex: 0
    keys:
      - 9rXVaIYKFoEwZkY3eDPHneNVVL...
```

```json
{
  "encryptionService": {
    "class": "com.mx.path.service.facility.encryption.jasypt.JasyptEncryptionService",
    "configurations": {
      "enabled": true,
      "poosSize": 10,
      "currentKeyIndex": 0
      "keys": ["9rXVaIYKFoEwZkY3eDPHneNVVL..."]
    }
  }
}
```

## Contributing
Create a topic branch. Make our changes commit and push to Github. Create an MR.

To test changes locally without generating a new version of the jar, you can package and install the jar locally by running:

```
$ ./gradlew clean build
```

# Managing Dependencies

This project uses gradle to manage dependencies. The Vogue plugin is used to help keep the dependencies up-to-date. [Vogue](https://github.com/mxenabled/vogue)

**View out-of-date dependencies for release**

```shell
$ ./gradlew vogueReport
```

**Determine source of dependency**

```shell
$ ./gradlew dependencies
```

**View out-of-date plugins**

Plugin versions need to be updated manually. This will show the outdated plugins:

```shell
$ ./gradlew dependencyUpdates
```

## Deploying

* Merge Pull Request to Master
* Switch to `master` branch
* Update version in `build.gradle` (the version must be unique)
* Commit the updated `build.gradle`
    * `git add build.gradle&&git commit -m "Bump version to ?.?.?"`
* Push build.gradle update
    * `git push origin master`
* Release it `rake release`
