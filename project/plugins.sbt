addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.4")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.11")

addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")

addSbtPlugin("io.chrisdavenport" % "sbt-no-publish" % "0.1.0")

ThisBuild / libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
