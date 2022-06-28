val Http4sVersion = "0.23.12"
val CirceVersion = "0.14.2"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.10"
val MunitCatsEffectVersion = "1.0.7"
val JsonSchemaValidatorVersion = "2.2.14"
val PureConfigVersion = "0.17.1"

lazy val root = (project in file("."))
  .settings(
    organization := "org.zarucki",
    name := "json-validation-service",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "org.http4s"                %% "http4s-ember-server"    % Http4sVersion,
      "org.http4s"                %% "http4s-ember-client"    % Http4sVersion,
      "org.http4s"                %% "http4s-circe"           % Http4sVersion,
      "org.http4s"                %% "http4s-dsl"             % Http4sVersion,
      "io.circe"                  %% "circe-generic"          % CirceVersion,
      "io.circe"                  %% "circe-literal"          % CirceVersion,
      "com.github.pureconfig"     %% "pureconfig"             % PureConfigVersion,
      "org.scalameta"             %% "munit"                  % MunitVersion                % Test,
      "org.typelevel"             %% "munit-cats-effect-3"    % MunitCatsEffectVersion      % Test,
      "ch.qos.logback"             %  "logback-classic"       % LogbackVersion              % Runtime,
      "com.github.java-json-tools" %  "json-schema-validator" % JsonSchemaValidatorVersion
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  )

// because in current implementation it doesn't wait on teardown cleaning
// FIXME
Test / parallelExecution := false