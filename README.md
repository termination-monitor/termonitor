# Termonitor

Pod **Ter**mination **Monitor** is a utility to monitor pod termination.
Currently, it measures how much of the termination grace period the pod needs to terminate.
When it needs more than the configured limit, a warning will be raised in the form of a Kubernetes Event.
Especially for stateful applications it is important to shut down cleanly and in time.
Being force-killed after the termination grace period is over might result in an unclean shutdown when not all connections are closed cleanly, not all data are saved.
It can also cause prolonged startup of sa new pod which might for example recreate some indexes or repair some files _damaged_ in the unclean shutdown.
(The exact issues might differ and depend on the exact information)

## Configuration



## Installation



## Getting help

If you encounter any issues while using this project, you can get help using [GitHub Discussions](https://github.com/scholzj/termonitor/discussions)

## Contributing

All contributions and ideas are welcomed!

## License

This project is licensed under the [version 2.0 of the Apache License](./LICENSE).

## Building and Testing

This project is written in Java and uses Quarkus Java Framework.
If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

### Running the application in dev mode

You can run your application in dev mode from your local environment:

```shell script
mvn compile quarkus:dev
```
