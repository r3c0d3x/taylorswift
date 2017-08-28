package modules

import java.io.IOException
import java.net.URL
import java.util.Scanner

import irc.config.UserConfig
import irc.message.Message
import irc.server.ServerResponder
import ircbot.{BotCommand, BotModule}
import org.json.{JSONException, JSONObject}


class Google extends BotModule {

  var cooldown = System.currentTimeMillis()
  val cooldownMS = 20000

  override def parse(m: Message, b: BotCommand, r: ServerResponder): Unit = {
    var target = m.target
    if (!m.params.first.startsWith("#")) target = m.sender.nickname
    if (b.command == "g" || b.command == "google") {
      if (b.paramsArray.length == 0) return
      if (cooldown > System.currentTimeMillis()){
        r.notice(m.sender.nickname, s"Search is currently on cooldown. Please wait another ${((cooldown - System.currentTimeMillis())/1000).toInt} seconds")
        return
      }
      val apiKey = UserConfig.getJson.getString("googlekey")
      val searchID = UserConfig.getJson.getString("searchid")
      val query = b.paramsString
      try {
        val url = new URL(s"https://www.googleapis.com/customsearch/v1?cx=$searchID&q=${query.replaceAll("\\s+", "%20")}&key=$apiKey")
        val in = url.openStream()
        val scan = new Scanner(in)
        var jsonstring = ""
        while (scan.hasNext) {
          jsonstring += scan.next() + " "
        }
        scan.close()
        val json = new JSONObject(jsonstring)
        val result = json.getJSONArray("items").getJSONObject(0)
        var title = result.getString("title")
        if (title.length > 100) title = title.substring(0, 99).trim() + "..."
        val snippet = result.getString("snippet").replace("\\n","").replace("\n", "")
        val link = result.getString("link")
        r.reply(s"Results for: \u0002$query \u0002")
        r.reply(title)
        r.reply(snippet)
        r.reply(link)

      } catch {
        case e: IOException => e.printStackTrace()
        case e: JSONException =>
          val response = "No results found for \u0002" + b.paramsString
          r.reply(response)
      }
    }
  }
}
