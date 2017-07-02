# FlaPyScala
Scala companion to [FlaPyDisaster](https://github.com/cliftbar/FlaPyDisaster).  This will primarily be a server for running calculations,
moving that burden from Python and the main application server to Scala.  This is being build using the Play framework.

Note that the JVM should be given the parameters
```
-Xms512M -Xmx2048M -Xss1M -XX:+CMSClassUnloadingEnabled
```
to run large/high resolution storms.  The IntelliJ default is `-Xmx1024`, which isn't enough memory for storms like Matthew 2016 at 100px per degree.
