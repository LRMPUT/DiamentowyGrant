package org.dg.globalSynchronization;

public class GraphOptimization {

	public GraphOptimization() {
		System.loadLibrary("g2o_stuff");
		System.loadLibrary("g2o_core");
		System.loadLibrary("g2o_ext_csparse");
		System.loadLibrary("g2o_csparse_extension");
		System.loadLibrary("g2o_solver_csparse");
		System.loadLibrary("g2o_module");
	}
}
