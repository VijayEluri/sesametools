package net.fortytwo.sesametools.sesamize;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Joshua Shinavier (http://fortytwo.net).
 */
public class SesamizeArgs {
    public final Set<String> flags;
    public final Map<String, String> pairs;
    public final List<String> nonOptions;

    private static final Map<String, RDFFormat> formatsByFileExtension;
    private static final Map<String, RDFFormat> formatsByName;

    static {
        formatsByFileExtension = new HashMap<>();
        formatsByName = new HashMap<>();
        for (RDFFormat format : RDFParserRegistry.getInstance().getKeys()) {
            for (String ext : format.getFileExtensions()) {
                formatsByFileExtension.putIfAbsent(ext, format);
            }
            formatsByName.put(format.getName().toLowerCase(), format);
        }
    }

    public SesamizeArgs(final String[] args) {
        flags = new HashSet<>();
        pairs = new HashMap<>();
        nonOptions = new LinkedList<>();

        boolean inOption = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("-")) {
                if (inOption) {
                    flags.add(getOptionName(args[i - 1]));
                } else {
                    inOption = true;
                }
            } else {
                if (inOption) {
                    pairs.put(getOptionName(args[i - 1]), a);
                    inOption = false;
                } else {
                    nonOptions.add(a);
                }
            }
        }

        if (inOption) {
            flags.add(getOptionName(args[args.length - 1]));
        }
    }

    public String getOption(final String defaultValue,
                            final String... alternatives) {
        for (String s : alternatives) {
            if (flags.contains(s)) {
                return "true";
            }

            String o = pairs.get(s);
            if (null != o) {
                return o;
            }
        }

        return defaultValue;
    }

    public SparqlResultFormat getSparqlResultFormat(final SparqlResultFormat defaultValue,
                                                    final String... alternatives) {
        String s = getOption(null, alternatives);
        return null == s ? defaultValue : SparqlResultFormat.lookupByNickname(s.toLowerCase());
    }

    public RDFFormat getRDFFormat(final RDFFormat defaultValue,
                                  final String... alternatives) {
        String s = getOption(null, alternatives);

        // If they specified an option, try to find it out of the non-standard
        // list of descriptors in Sesamize.rdfFormatByName
        if (null != s) {
            return findRDFFormat(s);
        } else { // otherwise return the default value
            return defaultValue;
        }
    }

    public RDFFormat getRDFFormat(final File file,
                                  final RDFFormat defaultValue,
                                  final String... alternatives) {
        String s = getOption(null, alternatives);
        RDFFormat format;

        // If they specified an option, try to find it out of the non-standard
        // list of descriptors in Sesamize.rdfFormatByName
        if (null != s) {
            format = findRDFFormat(s);
        } else {
            // otherwise try to find the format based on the file name extension,
            // using the specified default value as a fallback
            String ext = FilenameUtils.getExtension(file.getName());
            return formatsByFileExtension.get(ext);
            //Optional<RDFFormat> f = RDFFormat.matchFileName(file.getName(), );
            //format = f.isPresent() ? f.get() : defaultValue;
        }

        return format;
    }

    private RDFFormat findRDFFormat(final String s) {
        RDFFormat format = formatsByName.get(s.toLowerCase());
        if (null != format) {
            return format;
        }

        Optional<RDFFormat> f;

        RDFParserRegistry registry = RDFParserRegistry.getInstance();
        f = registry.getFileFormatForMIMEType(s);

        if (!f.isPresent()) {
            f = registry.getFileFormatForFileName("example." + s);
        }
        
        if (!f.isPresent()) {
            throw new IllegalArgumentException("no matching RDF format for '" + s + "' (" +
                    "supported formats are: " + getFormats(registry) + ")");
        }

        return f.get();
    }

    private String getFormats(final RDFParserRegistry registry) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RDFFormat format : registry.getKeys()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(format.getName());
        }
        return sb.toString();
    }

    private String getOptionName(final String option) {
        if (option.startsWith("--")) {
            return option.substring(2);
        } else if (option.startsWith("-")) {
            return option.substring(1);
        } else {
            return option;
        }
    }
}
