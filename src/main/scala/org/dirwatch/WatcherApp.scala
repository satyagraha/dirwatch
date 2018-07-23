package org.dirwatch

import java.nio.file._
import java.nio.file.StandardWatchEventKinds._

import org.kohsuke.args4j.{Argument, CmdLineParser, Option => Opt}

import scala.collection.JavaConverters._

class WatchDir(dir: String, glob: String, commands: Seq[String]) extends Runnable {
  val fileSystem = FileSystems.getDefault
  val watchService = fileSystem.newWatchService
  val watchDir = Paths.get(dir)
  val watchKey = watchDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
  val globber = fileSystem.getPathMatcher(s"glob:$glob")

  def pollEvents(): Seq[WatchEvent[Path]] = for {
    key <- Option(watchService.take()).toSeq
    event <- {
      val events = key.pollEvents().asScala
      key.reset()
      events
    }
  } yield event.asInstanceOf[WatchEvent[Path]]

  def eventStream(): Stream[WatchEvent[Path]] = Stream.continually(pollEvents()).flatten

  override def run(): Unit = {
    eventStream() foreach { event =>
//      println(event.kind())
//      println(event.context())
      val filename = event.context()
      val filepath = watchDir.resolve(filename)
//      println(filepath)
      event.kind() match {
        case ENTRY_CREATE | ENTRY_MODIFY if globber.matches(filename) =>
          val commandsSubstituted = commands map (_
            .replaceAllLiterally("%d", watchDir.toString)
            .replaceAllLiterally("%f", filename.toString)
            .replaceAllLiterally("%p", filepath.toString))
          println(s"""executing: ${commandsSubstituted.mkString(" ")}""")
          val process = new ProcessBuilder().command(commandsSubstituted.asJava).inheritIO().start()
          val exitCode = process.waitFor()
          println(s"exitCode: $exitCode")
        case ENTRY_DELETE =>
        case _ =>
      }
    }
  }

}

class WatcherArgs {
  @Opt(name = "-d", aliases = Array("--dir"), usage = "directory", required = true)
  var dir = ""

  @Opt(name = "-g", aliases = Array("--glob"), usage = "glob pattern", required = true)
  var glob = ""

  @Argument
  var commands = new java.util.ArrayList[String]
}

object WatcherApp extends App {
  val arguments = new WatcherArgs
  val parser = new CmdLineParser(arguments)
  val dashesPos = args.indexOf("--")
  require(dashesPos != -1)
  val (switches, commands) = args.splitAt(dashesPos)
  parser.parseArgument(switches: _*)
  val watcher = new WatchDir(arguments.dir, arguments.glob, commands.drop(1))
  watcher.run()
}
