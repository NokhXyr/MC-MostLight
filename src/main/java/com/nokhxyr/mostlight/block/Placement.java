package com.nokhxyr.mostlight.block;

/** Façon dont une lampe se pose et s'oriente. */
public enum Placement {
    /** Suspendue sous un bloc, orientée vers le joueur. */
    HANGING,
    /** Fixée contre un mur. */
    WALL,
    /** Posée sur un bloc, orientée vers le joueur. */
    STANDING,
    /** Posée au sol, haute de deux blocs. */
    TALL,
    /** Fixée sur n'importe quelle face (sol, plafond, murs). */
    OMNI,
    /** Bloc plein. */
    CUBE
}
