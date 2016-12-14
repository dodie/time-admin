package code.util

import java.io.InputStream

import net.liftweb.http.StreamingResponse

/**
  * Created by suliatis on 14/12/16.
  */
object IO {

  def using[A, B <: {def close(): Unit}] (closeable: B) (f: B => A): A = try { f(closeable) } finally { closeable.close() }

  def xlsxResponse(in: InputStream, name: String): StreamingResponse = StreamingResponse(
    in, () => in.close, in.available,
    List(
      "Content-Type" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "Content-Disposition" -> ("attachment; filename=\"" + name + "\"")
    ),
    Nil, 200
  )
}
