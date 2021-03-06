'use strict';
import ActionTypes from '../constants/ActionTypes';

let defaultState = {
  location: {
    center: {lat: 47.567, lng: 7.6},
    zoom: 11,
    bounds: {
      lngMin: 0,
      lngMax: 1,
      latMin: 0,
      latMax: 1
    }
  },
};

export default function (state = defaultState, action) {
  switch (action.type) {
    case ActionTypes.MAP_LOCATION_CHANGED:
      return {...state, location: {center: action.center, zoom: action.zoom, bounds: action.bounds}};
    default:
      return state;
  }
}
