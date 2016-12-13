window.my_vaadin_ffx_ProteinViewer = function() {
	// Give extra options to the viewer.
	var options = {
		width : 'auto',
		height : 'auto',
		antialias : true,
		outline : true,
		quality : 'medium'
	};

	// Create the base viewer.
	var viewer = pv.Viewer(document.getElementById('proteinViewer'), options);

	this.onStateChange = function() {
		var url = this.getState().url;
		var displayMode = this.getState().displayMode;
		pv.io.fetchPdb(url, function(structure) {
			// Clear any existing structures from the view.
			viewer.clear();
			// Display the protein as a cartoon by default.
			viewer.renderAs('protein', structure, displayMode);
			// Auto-zoom the viewer so the whole structure is included.
			viewer.autoZoom();
		});
	};
	
	function setColorForAtom(go, atom, color) {
	    var view = go.structure().createEmptyView();
	    view.addAtom(atom);
	    go.colorBy(pv.color.uniform(color), view);
	}

	// variable to store the previously picked atom. Required for resetting the color
	// whenever the mouse moves.
	var prevPicked = null;
	// add mouse move event listener to the div element containing the viewer. Whenever
	// the mouse moves, use viewer.pick() to get the current atom under the cursor.
	parent.addEventListener('mousemove', function(event) {
	    var rect = viewer.boundingClientRect();
	    var picked = viewer.pick({ x : event.clientX - rect.left,
	                               y : event.clientY - rect.top });
	    if (prevPicked !== null && picked !== null &&
	        picked.target() === prevPicked.atom) {
	      return;
	    }
	    if (prevPicked !== null) {
	      // reset color of previously picked atom.
	      setColorForAtom(prevPicked.node, prevPicked.atom, prevPicked.color);
	    }
	    if (picked !== null) {
	      var atom = picked.target();
	      document.getElementById('picked-atom-name').innerHTML = atom.qualifiedName();
	      // get RGBA color and store in the color array, so we know what it was
	      // before changing it to the highlight color.
	      var color = [0,0,0,0];
	      picked.node().getColorForAtom(atom, color);
	      prevPicked = { atom : atom, color : color, node : picked.node() };

	      setColorForAtom(picked.node(), atom, 'red');
	    } else {
	      document.getElementById('picked-atom-name').innerHTML = '&nbsp;';
	      prevPicked = null;
	    }
	    viewer.requestRedraw();
	});
}