# MDX Error Reporter Honeybadger

Honey Badger error reporter dependency for Helios middlewares

[Path SDK Issues](https://gitlab.mx.com/groups/mx/money-experiences/path/-/issues?scope=all&utf8=%E2%9C%93&state=opened&label_name[]=Path%20SDK)

## Deploying Locally

To create a local build of the accessor to use in connector services use

```shell
$ ./gradlew install
```
This will create a local build in your local maven repository that you can
then reference in other services.

On OXS using gradle the default location for the local maven repository is
```shell
~/.m2/repository/com/mx/path/service/accesssor/qolo/
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
