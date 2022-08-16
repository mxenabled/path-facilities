[![pipeline status](https://gitlab.mx.com/path/java-path-facility-store-vault/badges/master/pipeline.svg)](https://gitlab.mx.com/path/java-path-facility-store-vault/commits/master)
[![coverage report](https://gitlab.mx.com/path/java-path-facility-store-vault/badges/master/coverage.svg)](https://gitlab.mx.com/path/java-path-facility-store-vault/commits/master)

[Path SDK Issues](https://gitlab.mx.com/groups/mx/money-experiences/path/-/issues?scope=all&utf8=%E2%9C%93&state=opened&label_name[]=Path%20SDK)

# MDX Dependency - Store Vault

Vault store implementation

## Gateway Configuration

```yaml
vaultStore:
  class: "com.mx.path.facility.store.vault.VaultStore"
  configurations:
    enabled: true
    uri: http://127.0.0.1:8200
    authentication: APPID
    app-id: wedge
    user-id: 1f2cef4b6fe846fd86a4f6730ab74106
```

```json
{
  "vaultStore": {
    "class": "com.mx.path.facility.store.vault.VaultStore",
    "configurations": {
      "enabled": true,
      "uri": "http://127.0.0.1:8200",
      "authentication":  "APPID",
      "app-id": "wedge",
      "user-id": "1f2cef4b6fe846fd86a4f6730ab74106",
    }
  }
}
```

## Contributing
Create a topic branch. Make our changes commit and push to GitLab. Create an MR.

To test changes locally without generating a new version of the jar, you can package and install the jar locally by running:

```
$ ./gradlew clean build
```

# Managing Dependencies

This project uses gradle to manage dependencies. The Gradle Versions Plugin is used to help keep the dependencies up-to-date. [Gradle Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/index.html)

**View out-of-date dependencies for release**

```shell
$ ./gradlew dependencyUpdates -Drevision=release
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
