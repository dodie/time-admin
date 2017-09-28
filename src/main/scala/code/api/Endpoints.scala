package code.api

import net.liftweb.http.rest.RestHelper
import net.liftweb.json.JsonAST.JObject
import net.liftweb.util.Helpers.AsInt
import net.liftweb.json.JsonAST.JField
import net.liftweb.json.JsonAST.JString
import net.liftweb.json.JsonAST.JInt

object Endpoints extends RestHelper {

  serve {
    //curl http://localhost:8080/api/tasks
    case "api" :: "tasks" :: Nil JsonGet req => Some(JObject())
    
    //curl http://localhost:8080/api/taskitems/20170601-20170602
    case "api" :: "taskitems" :: range :: Nil JsonGet req => Some(JObject(JField("range", JString(range))))
    
    //curl -X POST -H "Content-Type: application/json" -d '{"key1":"value1", "key2":"value2"}' http://localhost:8080/api/taskitems
    case "api" :: "taskitems" :: Nil JsonPost ((jsonData, req)) => Some(JObject(JField("range", jsonData)))
    
    //curl -X PUT -H "Content-Type: application/json" -d '{"key1":"value1", "key2":"value2"}' http://localhost:8080/api/taskitems/123
    case "api" :: "taskitems" :: AsInt(id) :: Nil JsonPut ((jsonData, req)) => Some(JObject(JField("range", jsonData), JField("id", JInt(id))))
    
    //curl -X DELETE http://localhost:8080/api/taskitems/123
    case "api" :: "taskitems" :: AsInt(id) :: Nil JsonDelete req => Some(JObject(JField("id-to-delete", JInt(id))))
  }
}