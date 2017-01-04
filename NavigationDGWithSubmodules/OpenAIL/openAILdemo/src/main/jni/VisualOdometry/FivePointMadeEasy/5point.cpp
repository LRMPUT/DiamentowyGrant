#include "5point.h"
#include "Polynomial.h"
#include "Rpoly.h"

// Enable this for a more robust test against outliers
//#ifdef ROBUST_TEST

using namespace std;
using namespace Eigen;

static void ProjectionsFromEssential(const EMatrix &E, PMatrix &P1, PMatrix &P2, PMatrix &P3, PMatrix &P4);
static Vector4d TriangulatePoint(double x1, double y1, double x2, double y2, const PMatrix &P1, const PMatrix &P2);
static double CalcDepth(const Vector4d &X, const PMatrix &P);

bool Solve5PointEssential(double *pts1, double *pts2, int num_pts, std::vector<EMatrix> &ret_E, std::vector<PMatrix> &ret_P, std::vector<int> &ret_inliers)
{
    ret_E.clear();
    ret_P.clear();
    ret_inliers.clear();

    if(num_pts < 5) {
        return false;
    }

    // F is a temp variable, not the F fundamental matrix
    Matrix<double, Dynamic, 9> F(num_pts,9);
    for(int i=0; i < num_pts; i++) {
        double x1 = pts1[i*2];
        double y1 = pts1[i*2+1];

        double x2 = pts2[i*2];
        double y2 = pts2[i*2+1];
        F(i,0) = x1*x2;
        F(i,1) = x2*y1;
        F(i,2) = x2;
        F(i,3) = x1*y2;
        F(i,4) = y1*y2;
        F(i,5) = y2;
        F(i,6) = x1;
        F(i,7) = y1;
        F(i,8) = 1.0;
    }

    JacobiSVD<Matrix<double, Dynamic, 9> >    svd(F, ComputeFullV);

    const double e00 = svd.matrixV()(0,5),
                 e01 = svd.matrixV()(1,5),
                 e02 = svd.matrixV()(2,5),
                 e03 = svd.matrixV()(3,5),
                 e04 = svd.matrixV()(4,5),
                 e05 = svd.matrixV()(5,5),
                 e06 = svd.matrixV()(6,5),
                 e07 = svd.matrixV()(7,5),
                 e08 = svd.matrixV()(8,5),

                 e10 = svd.matrixV()(0,6),
                 e11 = svd.matrixV()(1,6),
                 e12 = svd.matrixV()(2,6),
                 e13 = svd.matrixV()(3,6),
                 e14 = svd.matrixV()(4,6),
                 e15 = svd.matrixV()(5,6),
                 e16 = svd.matrixV()(6,6),
                 e17 = svd.matrixV()(7,6),
                 e18 = svd.matrixV()(8,6),

                 e20 = svd.matrixV()(0,7),
                 e21 = svd.matrixV()(1,7),
                 e22 = svd.matrixV()(2,7),
                 e23 = svd.matrixV()(3,7),
                 e24 = svd.matrixV()(4,7),
                 e25 = svd.matrixV()(5,7),
                 e26 = svd.matrixV()(6,7),
                 e27 = svd.matrixV()(7,7),
                 e28 = svd.matrixV()(8,7),

                 e30 = svd.matrixV()(0,8),
                 e31 = svd.matrixV()(1,8),
                 e32 = svd.matrixV()(2,8),
                 e33 = svd.matrixV()(3,8),
                 e34 = svd.matrixV()(4,8),
                 e35 = svd.matrixV()(5,8),
                 e36 = svd.matrixV()(6,8),
                 e37 = svd.matrixV()(7,8),
                 e38 = svd.matrixV()(8,8);

    // Out symbolic polynomial matrix
    PolyMatrix M(10,10);

    // This file is not pretty to look at ...
    #include "Mblock.h"

    // symbolic determinant using interpolation based on the papers:
    // "Symbolic Determinants: Calculating the Degree", http://www.cs.tamu.edu/academics/tr/tamu-cs-tr-2005-7-1
    // "Multivariate Determinants Through Univariate Interpolation", http://www.cs.tamu.edu/academics/tr/tamu-cs-tr-2005-7-2

    // max power of the determinant is x^10, so we need 11 points for interpolation
    // the 11 points are at x = [-5, -4 .... 4, 5], luckily there is no overflow at x^10

    Matrix<double, 11, 11>              X;
    Matrix<double, 11, 1>               b;
    Matrix<double, 10, 10, RowMajor>    ret_eval;
    X.col(0).fill(1);

    // first column of M is the lowest power
    for(int i=-5, j=0; i <= 5; i++, j++) {
        M.Eval(i, ret_eval.data());
        double t = i;
        for(int k=1; k < 11; k++) {
            X(j,k) = t;
            t *= i;
        }
        b(j,0) = ret_eval.determinant();
    }

    // Using full pivot LU inverse, as partial pivot LU inverse (the default) generates less accurate inverses
    Matrix<double, 11, 1>   a = X.fullPivLu().inverse()*b;

    // Solve for z
    int degrees = 10;
    double coeffs[11];
    double zeror[11], zeroi[11];
    VectorXd solutions;

    // rpoly_ak1 expects highest power first
    for(int i=0; i < a.size(); i++)
        coeffs[i] = a(a.size()-i-1);

    // Find roots of polynomial
    rpoly_ak1(coeffs, &degrees, zeror, zeroi);

    for(int i=0; i < degrees; i++) {
        if(zeroi[i] == 0) {
            solutions.conservativeResize(solutions.size()+1);
            solutions(solutions.size()-1) = zeror[i];
        }
    }

    if(solutions.size() < 1) {
        return false;
    }

    // Back substitute the z values and compute null space to get x,y
    EMatrix E;
    PMatrix P_ref = PMatrix::Identity();
    PMatrix P[4];
    Vector4d pt3d;
    Vector3d x1, x2;

    int valid_solutions = 0;
    int best_inliers = 0;

    JacobiSVD<Matrix<double, 10, 10, RowMajor> >    svd2(10,10);
    for(int i=0; i < solutions.size(); i++) {
        double z = solutions(i);

        M.Eval(z, ret_eval.data());

        // Extract svd full V
        svd2.compute(ret_eval, ComputeFullV);

        // svd2.matrixV().col(9) represents
        // [x^3 , y^3 , x^2 y, xy^2 , x^2 , y^2 , xy, x, y, 1]^T

        // Scale it so the last element is 1, to get the correct answer
        double x = svd2.matrixV()(7,9) / svd2.matrixV()(9,9);
        double y = svd2.matrixV()(8,9) / svd2.matrixV()(9,9);

        // Build the essential matrix from all the known x,y,z values
        E(0,0) = e00*x + e10*y + e20*z + e30;
        E(0,1) = e01*x + e11*y + e21*z + e31;
        E(0,2) = e02*x + e12*y + e22*z + e32;

        E(1,0) = e03*x + e13*y + e23*z + e33;
        E(1,1) = e04*x + e14*y + e24*z + e34;
        E(1,2) = e05*x + e15*y + e25*z + e35;

        E(2,0) = e06*x + e16*y + e26*z + e36;
        E(2,1) = e07*x + e17*y + e27*z + e37;
        E(2,2) = e08*x + e18*y + e28*z + e38;

        // Test to see if this E matrix is the correct one we're after
        ProjectionsFromEssential(E, P[0], P[1], P[2], P[3]);

#ifdef ROBUST_TEST
        // Robust chirality test to handle outliers
        for(int j=0; j < 4; j++) {
            int inliers=0;
            for(int k=0; k < num_pts; k++) {
                TriangulatePoint(pt3d, pts1[k*2], pts1[k*2+1], pts2[k*2], pts2[k*2+1], P_ref, P[j]);
                x1 = P_ref * pt3d;
                x2 = P[j]  * pt3d;
                int s   = (pt3d(3,0) < 0 ? -1 : 1),
                    s1  =   (x1(2,0) < 0 ? -1 : 1),
                    s2  =   (x2(2,0) < 0 ? -1 : 1);
                if( (s1 + s2)*s == 2)
                    inliers++;
                // If any outliers for 5 points or 75% outliers for larger num_pts then reject the solution as probably invalid
                if( (num_pts == 5 && inliers <= k) || (inliers < k/4) ) {
                    inliers = 0;
                    break;
                }
            }
            if(inliers >= best_inliers && (inliers >= 5)) {
                // Add this solution to the valid solutions list
                if( inliers > best_inliers )        // If this has more inliers, it becomes the only valid solution
                    valid_solutions = 0;
                best_inliers = inliers;
                AllE[valid_solutions] = E;
                AllP[valid_solutions] = P[j];
                if( valid_solutions < 10 )          // Should never exceed 10 unless something has gone badly wrong
                    ++valid_solutions;
            }
        }
#else
        EMatrix best_E;
        PMatrix best_P;
        int best_inliers = 0;
        bool found = false;

        for(int j=0; j < 4; j++) {
            pt3d = TriangulatePoint(pts1[0], pts1[1], pts2[0], pts2[1], P_ref, P[j]);
            double depth1 = CalcDepth(pt3d, P_ref);
            double depth2 = CalcDepth(pt3d, P[j]);

            if(depth1 > 0 && depth2 > 0){
                int inliers = 1; // number of points in front of the camera

                for(int k=1; k < num_pts; k++) {
                    pt3d = TriangulatePoint(pts1[k*2], pts1[k*2+1], pts2[k*2], pts2[k*2+1], P_ref, P[j]);
                    depth1 = CalcDepth(pt3d, P_ref);
                    depth2 = CalcDepth(pt3d, P[j]);

                    if(depth1 > 0 && depth2 > 0) {
                        inliers++;
                    }
                }

                if(inliers > best_inliers && inliers >= 5) {
                    best_inliers = inliers;

                    best_E = E;
                    best_P = P[j];
                    found = true;
                }

                // Special case, with 5 points you can get a perfect solution
                if(num_pts == 5 && inliers == 5) {
                    break;
                }
            }
        }

        if(found) {
        ret_E.push_back(best_E);
        ret_P.push_back(best_P);
        ret_inliers.push_back(best_inliers);
        }
#endif
    }

    if(ret_E.size()) {
        return true;
    }

    return false;
}

static void ProjectionsFromEssential(const EMatrix &E, PMatrix &P1, PMatrix &P2, PMatrix &P3, PMatrix &P4)
{
    // Assumes input E is a rank 2 matrix, with equal singular values
    JacobiSVD<EMatrix> svd(E, ComputeFullU | ComputeFullV);
    const Matrix3d &U = svd.matrixU(),
                   &V = svd.matrixV();
    Matrix3d W;

	// Find rotation, translation
    W.setZero();
    W(0,1) = -1.0;
    W(1,0) = 1.0;
    W(2,2) = 1.0;

    // Rotation
    Matrix3d R1 = U * W             * V.transpose();
    Matrix3d R2 = U * W.transpose() * V.transpose();

//   if( R1.determinant() < 0 )
//        R1 *= -1;
//    if( R2.determinant() < 0 )
//        R2 *= -1;

    P1.block(0,0,3,3) = R1;
    P2.block(0,0,3,3) = R1;
    P3.block(0,0,3,3) = R2;
    P4.block(0,0,3,3) = R2;

    // Translation
    P1.col(3) =  U.col(2);
    P2.col(3) = -U.col(2);
    P3.col(3) =  U.col(2);
    P4.col(3) = -U.col(2);
}

// X is 4x1 is [x,y,z,w]
// P is 3x4 projection matrix
double CalcDepth(const Vector4d &X, const PMatrix &P)
{
    // back project
    Vector3d X2 = P*X;

    double det = P.block(0,0,3,3).determinant();
    double w = X2(2,0);
    double W = X(3,0);

    double a = P(0,2);
    double b = P(1,2);
    double c = P(2,2);

    double m3 = sqrt(a*a + b*b + c*c);  // 3rd column of M

    double sign;

    if(det > 0) {
        sign = 1;
    }
    else {
        sign = -1;
    }

    return (w/W)*(sign/m3);
}

static Vector4d TriangulatePoint(double x1, double y1, double x2, double y2, const PMatrix &P1, const PMatrix &P2)
{
    Matrix4d A;

    A(0,0) = x1*P1(2,0) - P1(0,0);
    A(0,1) = x1*P1(2,1) - P1(0,1);
    A(0,2) = x1*P1(2,2) - P1(0,2);
    A(0,3) = x1*P1(2,3) - P1(0,3);

    A(1,0) = y1*P1(2,0) - P1(1,0);
    A(1,1) = y1*P1(2,1) - P1(1,1);
    A(1,2) = y1*P1(2,2) - P1(1,2);
    A(1,3) = y1*P1(2,3) - P1(1,3);

    A(2,0) = x2*P2(2,0) - P2(0,0);
    A(2,1) = x2*P2(2,1) - P2(0,1);
    A(2,2) = x2*P2(2,2) - P2(0,2);
    A(2,3) = x2*P2(2,3) - P2(0,3);

    A(3,0) = y2*P2(2,0) - P2(1,0);
    A(3,1) = y2*P2(2,1) - P2(1,1);
    A(3,2) = y2*P2(2,2) - P2(1,2);
    A(3,3) = y2*P2(2,3) - P2(1,3);

    JacobiSVD<Matrix4d> svd(A, ComputeFullV);

    return svd.matrixV().col(3);
}
