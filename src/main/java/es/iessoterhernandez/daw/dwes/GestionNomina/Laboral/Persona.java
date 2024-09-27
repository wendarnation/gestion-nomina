package es.iessoterhernandez.daw.dwes.GestionNomina.Laboral;

// Clase Persona

public class Persona {

	public String nombre, dni;
	public char sexo;
	
	public Persona(String nombre, String dni, char sexo) {
		super();
		this.nombre = nombre;
		this.dni = dni;
		this.sexo = sexo;
	}
	
	public Persona(String nombre, char sexo) {
		super();
		this.nombre = nombre;
		this.sexo = sexo;
	}
	
	
	public void setDni(String nuevoDni) {
		dni = nuevoDni;
	}
	
	public void imprime() {
		System.out.println(nombre + ", " + dni);
	}
}
