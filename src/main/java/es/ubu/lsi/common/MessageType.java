package es.ubu.lsi.common;

import java.io.Serializable;

/**
 * Tipos de mensajes que se manejan en el chat.
 */
public enum MessageType implements Serializable {
    /** Mensaje de texto normal. */
    MESSAGE,
    /** Mensaje para desconectar al cliente. */
    LOGOUT,
    /** Reservado para usos futuros. */
    SHUTDOWN
}