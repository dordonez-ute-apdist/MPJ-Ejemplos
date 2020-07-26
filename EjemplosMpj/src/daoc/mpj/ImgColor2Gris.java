package daoc.mpj;

import mpi.MPI;

public class ImgColor2Gris {

	/**
	 * Transforma, en paralelo, una imagen color en una gris
	 * El n�mero de procesos *debe ser par*
	 * @param args
	 */
	public static void main(String[] args) {
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int tag = 1;
		int master = 0;
		int[][] limites = null;
		//todos cargan la imagen para lectura
		Matriz[] rgb = Matriz.desdeImagen(Matriz.IMG_PATH_03);
		//el master divide el trabajo entre todos
		//a cada proceso le da los l�mites de un bloque para procesar		
		int[] mislimites = new int[4];
		if(rank == master ) {
			limites = rgb[0].limitesDivisionBloques(size);
			mislimites = limites[master];
			for(int i = 1; i < size; i++) {
				MPI.COMM_WORLD.Send(limites[i], 0, 4, MPI.INT, i, tag);
			}
		} else {
			MPI.COMM_WORLD.Recv(mislimites, 0, 4, MPI.INT, master, tag);
		}		
		//todos procesan su parte (reutilizan rgb[0] para guardar los resultados)
		for(int f = mislimites[0]; f <= mislimites[1]; f++) {
			for(int c = mislimites[2]; c <= mislimites[3]; c++) {
				rgb[0].setValor(
					(rgb[0].getValor(f, c) + rgb[1].getValor(f, c) + rgb[2].getValor(f, c)) / 3,
					f, c);
			}
		}		
		Matriz[] gris = new Matriz[1];
		gris[0] = rgb[0].getSubMatriz(mislimites[0], mislimites[1], mislimites[2], mislimites[3]);
		//el master recolecta los bloques procesados y genera la imagen final
		if(rank == master ) {
			Matriz[] bloques = new Matriz[size];
			bloques[master] = gris[0];
			for(int i = 1; i < size; i++) {
				MPI.COMM_WORLD.Recv(bloques, i, 1, MPI.OBJECT, i, tag);
			}
			for(int i = 0; i < size; i++) {
				rgb[0].setSubMatriz(bloques[i], limites[i][0], limites[i][1], limites[i][2], limites[i][3]);
			}
			rgb[2] = rgb[1] = rgb[0];
			Matriz.haciaImagen(rgb, Matriz.IMG_PATH_03_B);
			System.out.println("Listo: " + Matriz.IMG_PATH_03_B);
		} else {
			MPI.COMM_WORLD.Send(gris, 0, 1, MPI.OBJECT, master, tag);
		}

		MPI.Finalize();
	}

}
