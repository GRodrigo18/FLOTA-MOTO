package com.example.util

/**
 * Reglas de la flota sin Android (se prueban en app/src/test).
 */
object FleetRules {

    /** Formas de ordenar la lista de Flota. */
    enum class Sort(val label: String) {
        NOMBRE("Nombre"),
        PLACA("Placa"),
        ESTADO("Estado"),
        DEUDA("Deuda")
    }

    /**
     * Compara nombres como lo haría una persona: "Moto 2" va antes que "Moto 10"
     * (el orden alfabético simple pondría "Moto 10" antes que "Moto 2").
     */
    fun naturalCompare(a: String, b: String): Int {
        val x = a.trim().lowercase()
        val y = b.trim().lowercase()
        var i = 0
        var j = 0
        while (i < x.length && j < y.length) {
            val cx = x[i]
            val cy = y[j]
            if (cx.isDigit() && cy.isDigit()) {
                val si = i
                val sj = j
                while (i < x.length && x[i].isDigit()) i++
                while (j < y.length && y[j].isDigit()) j++
                val nx = x.substring(si, i).trimStart('0')
                val ny = y.substring(sj, j).trimStart('0')
                if (nx.length != ny.length) return nx.length - ny.length
                val c = nx.compareTo(ny)
                if (c != 0) return c
            } else {
                if (cx != cy) return cx.compareTo(cy)
                i++
                j++
            }
        }
        return (x.length - i) - (y.length - j)
    }

    /** Orden de los estados: primero lo que necesita atención. */
    private fun estadoRank(estado: String): Int = when (estado) {
        "Por Vencer" -> 0
        "En Taller" -> 1
        "Vigente" -> 2
        "Inactivo" -> 3
        else -> 4
    }

    data class SortKey(val nombre: String, val placa: String, val estado: String, val deuda: Double)

    /** Ordena cualquier lista de motos según [sort]; el desempate siempre es por nombre. */
    fun <T> sortUnits(units: List<T>, sort: Sort, key: (T) -> SortKey): List<T> {
        val byName = Comparator<T> { a, b -> naturalCompare(key(a).nombre, key(b).nombre) }
        val comparator = when (sort) {
            Sort.NOMBRE -> byName
            Sort.PLACA -> Comparator<T> { a, b -> naturalCompare(key(a).placa, key(b).placa) }.then(byName)
            Sort.ESTADO -> compareBy<T> { estadoRank(key(it).estado) }.then(byName)
            Sort.DEUDA -> compareByDescending<T> { key(it).deuda }.then(byName)
        }
        return units.sortedWith(comparator)
    }

    data class GuaranteeCancel(val nuevaDeudaTotal: Double, val nuevaDeudaGarantia: Double, val anulado: Double)

    /**
     * Moto inactiva: el saldo de garantía que faltaba pagar deja de ser deuda.
     * Solo se quita la parte de garantía; otras deudas reales (cuotas, daños) se conservan.
     */
    fun cancelGuarantee(deudaTotal: Double, deudaGarantia: Double): GuaranteeCancel {
        val anulado = deudaGarantia.coerceAtLeast(0.0).coerceAtMost(deudaTotal.coerceAtLeast(0.0))
        return GuaranteeCancel(
            nuevaDeudaTotal = (deudaTotal - anulado).coerceAtLeast(0.0),
            nuevaDeudaGarantia = 0.0,
            anulado = anulado
        )
    }

    /** Placa normalizada: mayúsculas, sin espacios. */
    fun normalizePlaca(placa: String): String = placa.uppercase().filterNot { it.isWhitespace() }

    /**
     * Valida un cambio de nombre y placa. Devuelve el mensaje de error, o null si está bien.
     * [otherPlacas] = placas de las DEMÁS motos.
     */
    fun validateRename(nombre: String, placa: String, otherPlacas: Collection<String>): String? {
        val n = nombre.trim()
        val p = normalizePlaca(placa)
        return when {
            n.isEmpty() -> "Escribe el nombre de la moto."
            n.length > 30 -> "El nombre es muy largo (máximo 30 letras)."
            p.length < 3 -> "Escribe la placa completa."
            p.length > 10 -> "La placa es muy larga."
            otherPlacas.any { normalizePlaca(it) == p } -> "Ya hay otra moto con la placa $p."
            else -> null
        }
    }

    /** Una moto paga cuota del día solo si está trabajando (no inactiva ni en el taller). */
    fun debeCuota(estado: String): Boolean = estado != "Inactivo" && estado != "En Taller"
}
