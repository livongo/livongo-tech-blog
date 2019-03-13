/* Copyright (C) 2019 Livongo Corporation - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "processing-large-files"

version := "1.0.0"
scalaVersion := "2.12.8"

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-unchecked",
  "-feature",
  "-Ywarn-adapted-args",
  "-Ywarn-inaccessible",
  //"-Ywarn-unused",
  "-Ywarn-dead-code",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard",
  "-Ypartial-unification",
  "-Xfatal-warnings")

javacOptions ++= Seq("-Xdiags:verbose")

enablePlugins(PackPlugin)

// Akka Dependencies
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.19"

// F2 Dependencies
libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.7.1",
  "com.beachape" %% "enumeratum" % "1.5.13",
  "org.typelevel" %% "cats-core" % "1.5.0",
  "org.typelevel" %% "cats-effect" % "1.1.0",
  "co.fs2" %% "fs2-core" % "1.0.1",
  "co.fs2" %% "fs2-io" % "1.0.2"
)

// BetterFiles Dependencies
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.7.0"
