package water.fvec;

import org.junit.Test;
import water.Futures;
import water.Key;
import water.TestUtil;
import water.UKV;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by tomasnykodym on 3/28/14.
 */
public class SparseTest extends TestUtil {
  protected Chunk makeChunk(double [] vals){
    int nzs = 0;
    int [] nonzeros = new int[vals.length];
    int j = 0;
    for(double d:vals)if(d != 0)nonzeros[nzs++] = j++;
    Key key = Vec.newKey();
    AppendableVec av = new AppendableVec(key);
    NewChunk nv = new NewChunk(av,0);
    for(double d:vals){
      if(Double.isNaN(d))nv.addNA();
      else if((long)d == d) nv.addNum((long)d,0);
      else nv.addNum(d);
    }
    nv.close(0,null);
    Futures fs = new Futures();
    Vec vec = av.close(fs);
    fs.blockForPending();
    return vec.chunkForChunkIdx(0);
  }

  protected Chunk setAndClose(double val, int id, Chunk c){return setAndClose(new double[]{val},new int[]{id},c);}
  protected Chunk setAndClose(double [] vals, int [] ids, Chunk c){
    final int cidx = c.cidx();
    final Vec vec = c._vec;
    for(int i = 0; i < vals.length; ++i)
      c.set0(ids[i],vals[i]);
    Futures fs = new Futures();
    c.close(cidx,fs);
    return vec.chunkForChunkIdx(cidx);
  }

  public void runTest(double [] vs, double v1, double v2, Class class0, Class class1, Class class2){
    int nzeros = 3;
    int length = 4*NewChunk.MIN_SPARSE_RATIO + 1;
    double [] vals = new double[length];
    int [] nzs = new int[]{length/4,length/2,(3*length)/4};
    // test sparse double
    vals[nzs[0]] = vs[0];
    vals[nzs[1]] = vs[1];
    vals[nzs[2]] = vs[2];
    Chunk c0 = makeChunk(vals);
    assertTrue(class0.isAssignableFrom(c0.getClass()));
    try{
      assertTrue(class0.isAssignableFrom(c0.getClass()));
      assertEquals(3,c0.sparseLen());
      for(int i = 0; i < vals.length; ++i){
        assertEquals(Double.isNaN(vals[i]), c0.isNA0(i));
        assertTrue(Double.isNaN(vals[i]) || vals[i] == c0.at0(i));
      }
      int j = c0.nextNZ(-1);
      // test skip cnt iteration
      for(int nz:nzs){
        assertEquals(nz,j);
        assertEquals(Double.isNaN(vals[nz]),c0.isNA0(j));
        assertTrue(Double.isNaN(vals[nz]) || vals[nz] == c0.at0(j));
        j = c0.nextNZ(j);
      }
      Iterator<CXIChunk.Value> it = ((CXIChunk)c0).values();
      // test iterator
      for(int nz:nzs){
        CXIChunk.Value v = it.next();
        assertEquals(nz,v.rowInChunk());
        assertEquals(Double.isNaN(vals[nz]), v.isNA());
        assertTrue(Double.isNaN(vals[nz]) || vals[nz] == v.asDouble());
      }
      Chunk c1 = setAndClose(vals[length-1] = v1,length-1,c0);
      assertTrue(class1.isAssignableFrom(c1.getClass()));
      // test sparse set
      assertEquals(4,c1.sparseLen());
      assertEquals(Double.isNaN(v1),c1.isNA0(length-1));
      assertTrue(Double.isNaN(v1) || v1 == c1.at0(length-1));
      Chunk c2 = setAndClose(vals[0] = v2,0,c1);
      assertTrue(class2.isAssignableFrom(c2.getClass()));
      assertTrue(c2.nextNZ(-1) == 0);
      assertEquals(vals.length,c2.sparseLen());
      for(int i = 0; i < vals.length; ++i){
        assertEquals(Double.isNaN(vals[i]),c2.isNA0(i));
        assertTrue(Double.isNaN(vals[i]) || vals[i] == c2.at0(i));
        assertTrue(c2.nextNZ(i) == i+1);
      }
    }finally{
      UKV.remove(c0._vec._key);
    }
  }


  @Test
  public void testDouble() {runTest(new double [] {2.7182,3.14,42},Double.NaN,123.45,CXDChunk.class,CXDChunk.class,C8DChunk.class);}

  @Test
  public void testBinary() {
    runTest(new double [] {1,1,1},1,1,CX0Chunk.class,CX0Chunk.class,CBSChunk.class);
    runTest(new double [] {1,1,1},Double.NaN,1,CX0Chunk.class,CXIChunk.class,CBSChunk.class);
  }
  @Test public void testInt() {
    runTest(new double [] {1,2,Double.NaN},4,5,CXIChunk.class,CXIChunk.class,C1Chunk.class);
    runTest(new double [] {1,2000,Double.NaN,3},4,5,CXIChunk.class,CXIChunk.class,C2Chunk.class);
    runTest(new double [] {Double.NaN,2000,3},400000,5,CXIChunk.class,CXIChunk.class,C4Chunk.class);
    runTest(new double [] {1,Double.NaN,2000,3},Double.NaN,1e10,CXIChunk.class,CXIChunk.class,C8Chunk.class);
  }



}
