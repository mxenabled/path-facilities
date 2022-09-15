# MDX Dependency - Encryption Service Vault

Vault encryption service implementation

## Gateway Configuration

```yaml
encryptionService:
  class: "com.mx.path.facility.security.vault.VaultEncryptionService"
  configurations:
    enabled: true
    uri: http://127.0.0.1:8200
    authentication: APPID
    app-id: wedge
    user-id: 1f2cef4b6fe846fd86a4f6730ab74106
    numKeysToKeep: 5
```

```json
{
  "encryptionService": {
    "class": "com.mx.path.facility.security.vault.VaultEncryptionService",
    "configurations": {
      "enabled": true,
      "uri": "http://127.0.0.1:8200",
      "authentication":  "APPID",
      "app-id": "wedge",
      "user-id": "1f2cef4b6fe846fd86a4f6730ab74106",
      "numKeysToKeep": 5
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
