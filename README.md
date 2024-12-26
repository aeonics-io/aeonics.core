## Plugin: "aeonics.core"

This Aeonics software plugin provides the core implementation of the system.

## Compile and package

You can use your favourite tool (Maven, Gradle,...) but to be honest, we prefer
the plain simple standard and out-of-the-box `javac`.

The binary distribution of the *aeonics.boot* jar should be in the
current directory.

```shell
javac -source 11 -target 11 -nowarn -XDignore.symbol.file \
      -d aeonics.core/bin \
      --module-path . \
      --module-source-path .\
      --module aeonics.core

jar -c --file=aeonics.core.jar \
    -C aeonics.core/bin/aeonics.core \
    .
```

## Deployment

Place the binary distribution in the `plugins` folder of your installation.
