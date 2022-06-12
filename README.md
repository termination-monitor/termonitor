# Termonitor

Pod **Ter**mination **Monitor** is a utility to monitor pod termination.
Currently, it measures how much of the termination grace period the pod needs to terminate.
When it needs more than the configured limit, a warning will be raised in the form of a Kubernetes Event.
Especially for stateful applications it is important to shut down cleanly and in time.
Being force-killed after the termination grace period is over might result in an unclean shutdown when not all connections are closed cleanly, not all data are saved.
It can also cause prolonged startup of sa new pod which might for example recreate some indexes or repair some files _damaged_ in the unclean shutdown.
(The exact issues might differ and depend on the exact information)

## Configuration

Following configuration options are currently supported:

| Option      | Description                                                             | Default | Example        |
|:------------|:------------------------------------------------------------------------|:--------|:---------------|
| --threshold | The threshold (in % or grace period) at which an alert should be raised | 75      | --threshold=80 |

## Installation

You can install the last stable release of Termonitor using the YAML files in the [`./install`](./install) directory:

```shell
kubectl apply -f install/
```

This will create a new namespace `termonitor` and deploy Termonitor inside.
It will also create the RBAC files required by Termonitor.
If you want, you can customize the files to install it into a different namespace.
Termonitor currently doesn't support watching only selected namespaces.
It currently always watches the whole cluster.

## Getting help

If you encounter any issues while using this project, you can get help in [GitHub Discussions](https://github.com/scholzj/termonitor/discussions)

## Contributing

All contributions and ideas are welcomed!

## License

This project is licensed under the [version 2.0 of the Apache License](./LICENSE).

## Building and Testing

This project is written in Java and uses Quarkus Java Framework.
If you want to learn more about Quarkus, please visit its website: https://quarkus.io/.

### Building the latest version

You can use the `make` build to build the project.

### Running the latest version from the `main` branch

You can run the latest in-development version from the `main` GitHub branch using the files from the  [`./pckagin/install`](./packaging/install) directory:

```shell
kubectl apply -f packaging/install/
```

This will use the container image with the `:latest` tag.

If you need to make any changes to the Kubernetes installation files, you should also do them in the [`./pckagin/install`](./packaging/install) directory.
The [`./install`](./install) directory is updated only during releases.

### Running the application in dev mode

You can run your application in dev mode from your local environment:

```shell script
mvn compile quarkus:dev
```
