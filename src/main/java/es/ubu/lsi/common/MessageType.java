package es.ubu.lsi.common;



/**
 * Tipos de mensajes que se manejan en el chat.
 */
public enum MessageType  {
    /** Mensaje de texto normal. */
    MESSAGE,
    /** Mensaje para desconectar al cliente. */
    LOGOUT,
    /** Reservado para usos futuros. */
    SHUTDOWN
}