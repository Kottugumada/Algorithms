/**
 * LCS implementation.
 *
 * @author William Fiset, william.alexandre.fiset@gmail.com
 */

package com.williamfiset.algorithms.strings;

import java.util.*;

public class Lcs {

  private static abstract class SuffixArray {
    
    protected static final int DEFAULT_ALPHABET_SHIFT = 0;
    protected static final int DEFAULT_ALPHABET_SIZE = 256;
    
    // Length of the suffix array
    public final int N;
    
    // The shift value is used if you need to fake "shifting" the entire alphabet a certain
    // number of units either up or down. This is useful when you introduce sentinel values into
    // the suffix array which need to be lexicographically less than the rest the alphabet.
    // The shift value can also be useful to reduce the alphabet size if you know your alphabet will
    // only contain values between [a, b], then if your shift = a, your new alphabet can be
    // of a lower range [0, b - a]
    //
    // NOTE: It may be best to set the user shift the values of the text before supplying it to the
    // Suffix array that way the implementation never needs to worry about shifting. This is the 
    // default behavior anyways.
    protected int shift = DEFAULT_ALPHABET_SHIFT;
    
    protected int alphabetSize = DEFAULT_ALPHABET_SIZE;

    // T is the text
    public int[] T;

    // The sorted suffix array values.
    public int[] sa;
    
    // Longest Common Prefix array
    public int [] lcp;

    // Designated constructor
    public SuffixArray(int[] text, int shift, int alphabetSize) {
      if (text == null || alphabetSize <= 0) 
        throw new IllegalArgumentException();
      
      this.T = text;
      this.N = text.length;
      
      this.shift = shift;
      this.alphabetSize = alphabetSize;
      
      // Build suffix array
      construct();
      
      // Build LCP array
      kasai();
    }
    
    protected static int[] toIntArray(String s) {
      if (s == null) return null;
      int[] text = new int[s.length()];
      for(int i = 0; i < s.length(); i++)
        text[i] = s.charAt(i);
      return text;
    }
    
    // The suffix array construction algorithm is left undefined 
    // as there are multiple ways to do this.
    protected abstract void construct();

    // Use Kasai algorithm to build LCP array
    // http://www.mi.fu-berlin.de/wiki/pub/ABI/RnaSeqP4/suffix-array.pdf
    private void kasai() {
      lcp = new int[N];
      int [] inv = new int[N];
      for (int i = 0; i < N; i++) inv[sa[i]] = i;
      for (int i = 0, len = 0; i < N; i++) {
        if (inv[i] > 0) {
          int k = sa[inv[i]-1];
          while((i+len < N) && (k+len < N) && T[i+len] == T[k+len]) len++;
          lcp[inv[i]] = len;
          if (len > 0) len--;
        }
      }
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("------i------SA------LCP--------Suffix\n");

      for(int i = 0; i < N; i++) {
        int suffixLen = N - sa[i];
        char[] string = new char[suffixLen];
        for (int j = sa[i], k = 0; j < N; j++, k++)
          string[k] = (char)(T[j] - shift);
        String suffix = new String(string);
        String formattedStr = String.format("% 7d % 7d % 7d %s\n", i, sa[i], lcp[i], suffix);
        sb.append(formattedStr);
      }
      return sb.toString();
    }

    // Lazy way of finding color of suffix is by comparing against all sentinel positions
    private static Color findColorFromPos(int pos, List<Integer> sentinelIndexes) {
      Color[] colors = {Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.WHITE, Color.BLACK_BACKGROUND_BRIGHT};
      int colorIndex = 0;
      for (int tokenIndex : sentinelIndexes) {
        if (tokenIndex <= pos) colorIndex++;
      }
      if (colorIndex >= colors.length) {
        throw new IllegalStateException("Too many strings, not enough terminal colors :/");
      }
      return colors[colorIndex];
    }

    // Display an augmented colored SA representation for debugging LCS problem.
    public void display(List<Integer> sentinelIndexes) {
      System.out.println("------i------SA------LCP--------Suffix");
      for(int i = 0; i < N; i++) {
        int suffixLen = N - sa[i];
        char[] string = new char[suffixLen];
        for (int j = sa[i], k = 0; j < N; j++, k++)
          string[k] = (char)(T[j] - shift);
        String suffix = new String(string);

        System.out.print(findColorFromPos(sa[i], sentinelIndexes));
        String formattedStr = String.format("% 7d % 7d % 7d %s", i, sa[i], lcp[i], suffix);
        System.out.println(formattedStr + Color.RESET);
      }
    }

    // https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println
    //
    // Usage:
    // System.out.print(Color.CYAN);
    // System.out.println("111111111aaaaaaaaaaaaaaaa==============");
    // System.out.print(Color.RESET);
    enum Color {
      RESET("\033[0m"),

      BLACK("\033[0;30m"),
      RED("\033[0;31m"),
      GREEN("\033[0;32m"),
      YELLOW("\033[0;33m"),
      BLUE("\033[0;34m"),
      MAGENTA("\033[0;35m"),
      CYAN("\033[0;36m"),
      WHITE("\033[0;37m"),
      BLACK_BACKGROUND_BRIGHT("\033[0;100m");

      private final String code;

      Color(String code) {
        this.code = code;
      }

      @Override
      public String toString() {
        return code;
      }
    }

  }

  private static class SuffixArrayImpl extends SuffixArray {

    // Wrapper class to help sort suffix ranks
    static class SuffixRankTuple implements Comparable<SuffixRankTuple> {
      int firstHalf, secondHalf, originalIndex;

      // Sort Suffix ranks first on the first half then the second half
      @Override
      public int compareTo(SuffixRankTuple other) {
        int cmp = Integer.compare(firstHalf, other.firstHalf);
        if (cmp == 0) return Integer.compare(secondHalf, other.secondHalf);
        return cmp;
      }

      @Override
      public String toString() {
        return originalIndex + " -> (" + firstHalf + ", " + secondHalf + ")";
      }
    }

    public SuffixArrayImpl(String text) {
      super(toIntArray(text), DEFAULT_ALPHABET_SHIFT, DEFAULT_ALPHABET_SIZE);
    }

    public SuffixArrayImpl(int[] text) {
      super(text, DEFAULT_ALPHABET_SHIFT, DEFAULT_ALPHABET_SIZE);
    }

    // TODO(williamfiset): Get rid of these constructors in favor of
    // automatically detecting the alphabet size shift required
    public SuffixArrayImpl(String text, int shift) {
      super(toIntArray(text), shift, DEFAULT_ALPHABET_SHIFT);
    }
    public SuffixArrayImpl(int[] text, int shift) {
      super(text, shift, DEFAULT_ALPHABET_SIZE);
    }

    // Designated constructor
    public SuffixArrayImpl(int[] text, int shift, int alphabetSize) {
      super(text, shift, alphabetSize);
    }

    // Construct a suffix array in O(nlog^2(n))
    @Override 
    protected void construct() {
      sa = new int[N];

      // Maintain suffix ranks in both a matrix with two rows containing the
      // current and last rank information as well as some sortable rank objects
      int[][] suffixRanks = new int[2][N];
      SuffixRankTuple[] ranks = new SuffixRankTuple[N];

      // Assign a numerical value to each character in the text
      for (int i = 0; i < N; i++) {
        suffixRanks[0][i] = T[i];
        ranks[i] = new SuffixRankTuple();
      }

      // O(log(n))
      for(int pos = 1; pos < N; pos *= 2) {
        for(int i = 0; i < N; i++) {
          SuffixRankTuple suffixRank = ranks[i];
          suffixRank.firstHalf = suffixRanks[0][i];
          suffixRank.secondHalf = i+pos < N ? suffixRanks[0][i+pos] : -1;
          suffixRank.originalIndex = i;
        }

        // O(nlog(n))
        java.util.Arrays.sort(ranks);

        int newRank = 0;
        suffixRanks[1][ranks[0].originalIndex] = 0;

        for (int i = 1; i < N; i++ ) {
          SuffixRankTuple lastSuffixRank = ranks[i-1];
          SuffixRankTuple currSuffixRank = ranks[i];

          // If the first half differs from the second half
          if (currSuffixRank.firstHalf  != lastSuffixRank.firstHalf ||
              currSuffixRank.secondHalf != lastSuffixRank.secondHalf)
            newRank++;

          suffixRanks[1][currSuffixRank.originalIndex] = newRank;
        }

        // Place top row (current row) to be the last row
        suffixRanks[0] = suffixRanks[1];

        // Optimization to stop early
        if (newRank == N-1) break;
      }

      // Fill suffix array
      for (int i = 0; i < N; i++) {
        sa[i] = ranks[i].originalIndex;
        ranks[i] = null;
      }

      // Cleanup
      suffixRanks[0] = suffixRanks[1] = null;
      suffixRanks = null;
      ranks = null;
    }
  }

  private static class LcsSolver {

    // Inputs
    final int k, numSentinels, textLength;
    String[] strings;

    int lowestAsciiValue = Integer.MAX_VALUE;
    int highestAsciiValue = Integer.MIN_VALUE;

    // Output
    TreeSet<String> lcss = new TreeSet<>();

    public LcsSolver(String[] strings, int k) {
      if (strings == null || strings.length <= 1)
        throw new IllegalArgumentException("Invalid strings array provided.");
      if (k < 2)
        throw new IllegalArgumentException("k must be greater than or equal to 2");
      this.strings = strings;
      this.k = k;
      this.numSentinels = strings.length;
      this.textLength = computeTextLength(strings) + numSentinels;
    }

    // TODO(williamfiset): support LCS with strings as int arrays for larger alphabet sizes.

    private static int computeTextLength(String[] strings) {
      int len = 0;
      for (String str : strings) 
        len += str.length();
      return len;
    }

    // Builds a reverse color index map. The reverse color map tells you which
    // color a character is at a given index in the new text.
    private int[] buildReverseColorIndexMapping() {
      int[] imap = new int[textLength];
      for (int i = 0, k = 0; i < strings.length; i++) {
        String str = strings[i];
        for (int j = 0; j < str.length(); j++) {
          int asciiVal = str.charAt(j);
          if (asciiVal < lowestAsciiValue) lowestAsciiValue = asciiVal;
          if (asciiVal > highestAsciiValue) highestAsciiValue = asciiVal;
          imap[k++] = i;
        }
        // Record that the sentinel belongs to string i
        imap[k++] = i;
      }
      return imap;
    }

    // Build text containing sentinels. Must have computed lowest and highest ascii values beforehand.
    // All sentinels values will be in the range [0, numSentinels)
    // All text values will be in the range [numSentinels, numSentinels + highestAsciiValue - lowestAsciiValue]
    private int[] buildText() {
      int sentinel = 0;
      int shift = numSentinels - lowestAsciiValue;
      int[] text = new int[textLength];
      // Construct the new text with the shifted values and the sentinels
      for(int i = 0, k = 0; i < strings.length; i++) {
        String str = strings[i];
        for (int j = 0; j < str.length(); j++) {
          text[k++] = ((int)str.charAt(j)) + shift;
          if (!(numSentinels <= text[k-1] && text[k-1] <= (numSentinels + highestAsciiValue - lowestAsciiValue))) {
            throw new IllegalStateException(String.format("Unexpected character range. Was: %d, wanted between [%d, %d]", text[k-1], numSentinels, (numSentinels + highestAsciiValue - lowestAsciiValue)));
          }
        }
        text[k++] = sentinel++;
        if (!(0 <= text[k-1] && text[k-1] < numSentinels)) {
          throw new IllegalStateException(String.format("Unexpected character range. Was: %d, wanted between [%d, %d)", text[k-1], 0, numSentinels));
        }
      }
      return text;
    }

    private boolean enoughColorsInWindow(int lo, int hi, int[] imap, int[] sa) {
      Set<Integer> set = new HashSet<>();
      for (int i = lo; i < hi; i++) {
        set.add(imap[sa[i]]);
      }
      return set.size() >= k;
    }

    private String retrieveStrAt(int i, int windowLcp, int shift, int[] text) {
      char[] s = new char[windowLcp];
      for (int j = 0; j < windowLcp; j++) {
        s[j] = (char)(text[i + j] - shift);
      }
      return new String(s);
    }

    public void solve() {
      int[] imap = buildReverseColorIndexMapping();
      int[] text = buildText();
      
      SuffixArray suffixArray = new SuffixArrayImpl(text);
      int[] sa = suffixArray.sa;
      int[] lcp = suffixArray.lcp;

      // Add 10 extra spots to lcp array because seg tree is not inclusive on right endpoint
      // and we don't want index out of bounds.
      int[] lcp2 = new int[lcp.length + 10];
      for(int i = 0; i < lcp.length; i++) lcp2[i] = lcp[i];

      // TODO(williamfiset): Replace with SlidingWindowMinimum for speed.
      CompactMinSegmentTree tree = new CompactMinSegmentTree(lcp2);

      int lo = numSentinels;
      int hi = numSentinels;

      int lcsLen = 0;
      final int shift = numSentinels - lowestAsciiValue;
      System.out.println(java.util.Arrays.toString(lcp));

      while (true) {
        boolean shrinkWindow = (hi == textLength-1) ? true : enoughColorsInWindow(lo, hi, imap, sa);

        if (shrinkWindow) {
          lo++;
          if (lo == hi) break;

          int windowLcs = tree.query(lo+1, hi+1);
          System.out.printf("lo: %d, hi: %d, lcp: %d\n", lo, hi, windowLcs);
          if (windowLcs > lcsLen) {
            lcsLen = windowLcs;
            lcss.clear();
          }
          if (windowLcs == lcsLen) {
            // Add str to lcss
            lcss.add(retrieveStrAt(sa[lo], windowLcs, shift, text));
          }
        // Expand window
        } else {
          hi++;

          int windowLcs = tree.query(lo+1, hi+1);
          System.out.printf("lo: %d, hi: %d, lcp: %d\n", lo, hi, windowLcs);
          if (windowLcs > lcsLen) {
            lcsLen = windowLcs;
            lcss.clear();
          }
          if (windowLcs == lcsLen) {
            lcss.add(retrieveStrAt(sa[lo], windowLcs, shift, text));
          }
        }
      }
    }

  }

  private static class SlidingWindowMinimum {
    int[] values;
    int N, lo, hi;

    Deque<Integer> deque = new ArrayDeque<>();

    public SlidingWindowMinimum(int[] values) {
      if (values == null) throw new IllegalArgumentException();
      this.values = values;
      N = values.length;
    }

    // Advances the front of the window by one unit
    public void advance() {
      // Remove all the worse values in the back of the deque
      while(!deque.isEmpty() && values[deque.peekLast()] > values[hi])
        deque.removeLast();

      // Add the next index to the back of the deque
      deque.addLast(hi);

      // Increase the window size
      hi++;
    }

    // Retracks the back of the window by one unit
    public void shrink() {
      // Decrease window size by pushing it forward
      lo++;

      // Remove elements in the front of the queue whom are no longer
      // valid in the reduced window.
      while(!deque.isEmpty() && deque.peekFirst() < lo)
        deque.removeFirst();
    }

    // Query the current minimum value in the window
    public int getMin() {
      if (lo >= hi) throw new IllegalStateException("Make sure lo < hi");
      return values[deque.peekFirst()];
    }
  }


  private static class CompactMinSegmentTree {
    private int n;

    // Let UNIQUE be a value which does NOT and will NOT appear in the segment tree.
    private int UNIQUE = 93136074;

    // Segment tree values
    private int[] tree;

    public CompactMinSegmentTree(int size) {
      tree = new int[2*(n = size)];
      java.util.Arrays.fill(tree, UNIQUE);
    }

    public CompactMinSegmentTree(int[] values) {
      this(values.length);
      for(int i = 0; i < n; i++) modify(i, values[i]);
    }

    // The segment tree function used for queries.
    private int function(int a, int b) {
      if (a == UNIQUE) return b;
      else if (b == UNIQUE) return a;
      return (a < b) ? a : b; // minimum value over a range
    }

    // Adjust point i by a value, O(log(n))
    public void modify(int i, int value) {
      tree[i + n] = function(tree[i+n],  value);
      for (i += n; i > 1; i >>= 1) {
        tree[i>>1] = function(tree[i], tree[i^1]);
      }
    }
    
    // Query interval [l, r), O(log(n))
    public int query(int l, int r) {
      int res = UNIQUE;
      for (l += n, r += n; l < r; l >>= 1, r >>= 1) {
        if ((l&1) != 0) res = function(res, tree[l++]);
        if ((r&1) != 0) res = function(res, tree[--r]);
      }
      if (res == UNIQUE) {
        throw new IllegalStateException("UNIQUE should not be the return value.");
      }
      return res;
    }
  }


  // Method to add unique sentinels. Works for low number of strings.
  static String addSentinels(String[] s, List<Integer> sentinelIndexes) {
    int token = 35; // starts at '#'
    String t = "";
    for (String string : s) {
      t += string;
      t += (char) token;
      token++;
      if (sentinelIndexes != null)
        sentinelIndexes.add(t.length());
    }
    return t;
  }

  public static void main(String[] args) {
    String[] strings = new String[]{"TAAAAT", "ATAAAAT", "TATA", "ATA", "AAT", "TTTT", "TT"};
    List<Integer> sentinelIndexes = new ArrayList<>();
    String t = addSentinels(strings, sentinelIndexes);
    SuffixArray sa = new SuffixArrayImpl(t);

    sa.display(sentinelIndexes);
    LcsSolver solver = new LcsSolver(strings, 3);
    solver.solve();
    System.out.println(solver.lcss);

    // String T = "ABB#BABA$AAAB&";
    // List<Integer> sentinelIndexes = new ArrayList<>();
    // sentinelIndexes.add(3);
    // sentinelIndexes.add(8);
    // sentinelIndexes.add(T.length() - 1);
    // SuffixArray sa = new SuffixArrayImpl(T);

    // sa.display(sentinelIndexes);
  }
}


















