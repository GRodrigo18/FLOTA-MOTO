package com.example.util

/**
 * Cálculo del costo total de un trabajo de taller.
 *
 * Antes las llantas se sumaban DOS veces: una como "servicio" y otra como "llantas usadas del
 * inventario", así que 2 llantas de S/ 85 descontaban S/ 340 de la Caja en vez de S/ 170.
 * Ahora: total = mano de obra (servicios) + aceite usado + llantas usadas, cada cosa una sola vez.
 */
object MaintenanceMath {

    /**
     * @param servicios suma de los servicios marcados (general, frenos, caña, planchado, cables, motor, otros).
     *                  NO incluye aceite ni llantas.
     * @param costoTrabajo total que mostró el formulario; solo se usa si no se marcó ningún servicio.
     */
    fun manoDeObra(servicios: Double, costoTrabajo: Double, aceite: Double, llantas: Double): Double =
        if (servicios > 0.0) servicios else (costoTrabajo - aceite - llantas).coerceAtLeast(0.0)

    fun total(servicios: Double, costoTrabajo: Double, aceite: Double, llantas: Double): Double =
        manoDeObra(servicios, costoTrabajo, aceite, llantas) + aceite + llantas
}
